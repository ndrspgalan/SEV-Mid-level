package tests;

import accountHistory.AccountHistoryEventStatus;
import accountHistory.AccountHistoryEventType;
import application.lifecycle.AccountLifecycleRejectionReason;
import application.lifecycle.AccountLifecycleResult;
import application.lifecycle.AccountLifecycleService;
import application.operation.ExchangeOperationService;
import banking.census.ProfessionCatalog;
import banking.census.ProfessionCensus;
import banking.identity.HolderStatus;
import banking.lifecycle.*;
import coinProperties.Currency;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;
import exchangeCoin.ImplementedExchangePolicy;
import transaction.InMemoryTransactionLedger;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class AccountLifecycleTest {
    public static void main(String[] args) {
        blockAndUnblockAreHistoricalAndIdempotent();
        blockedAccountCannotExchange();
        closingRequiresZeroBalanceAndReleasesCensus();
        pendingHolderCannotBeBlocked();
        closedAccountIsTerminal();
        System.out.println("AccountLifecycleTest: OK");
    }

    private static ConsumerRegistry registry() {
        return new ConsumerRegistry(
                ProfessionCatalog.valerianStandard(),
                new ProfessionCensus(),
                new accountHistory.InMemoryAccountHistoryJournal(),
                Clock.fixed(Instant.parse("1456-01-30T10:00:00Z"), ZoneOffset.UTC)
        );
    }

    private static void blockAndUnblockAreHistoricalAndIdempotent() {
        ConsumerRegistry registry = registry();
        Consumer person = registry.register("Álvaro", "Carpintero");
        AccountLifecycleService service = new AccountLifecycleService(registry);
        AccountLifecycleRequestId requestId = AccountLifecycleRequestId.generate();
        AccountLifecycleRequest block = new AccountLifecycleRequest(
                requestId, person.getConsumerId(), AccountLifecycleAction.BLOCK, null, "FRAUD_REVIEW"
        );

        AccountLifecycleResult first = service.process(block);
        AccountLifecycleResult replay = service.process(block);
        check(first.isCompleted(), "block completed");
        check(person.getBankAccount().getOperationalStatus() == AccountOperationalStatus.BLOCKED, "account blocked");
        check(replay.isIdempotentReplay(), "same command is replayed");
        check(registry.getAccountHistoryJournal().findAll().size() == 2, "replay creates no duplicate event");

        AccountLifecycleResult conflict = service.process(new AccountLifecycleRequest(
                requestId, person.getConsumerId(), AccountLifecycleAction.UNBLOCK, null, "DIFFERENT_PAYLOAD"
        ));
        check(conflict.getRejectionReason().orElseThrow() == AccountLifecycleRejectionReason.IDEMPOTENCY_CONFLICT,
                "different payload conflicts");

        AccountLifecycleResult unblocked = service.process(new AccountLifecycleRequest(
                AccountLifecycleRequestId.generate(), person.getConsumerId(), AccountLifecycleAction.UNBLOCK, null, "REVIEW_COMPLETED"
        ));
        check(unblocked.isCompleted(), "unblock completed");
        check(person.getBankAccount().getOperationalStatus() == AccountOperationalStatus.ACTIVE, "account active again");
        var event = registry.getAccountHistoryJournal().findAll().get(2);
        check(event.type() == AccountHistoryEventType.ACCOUNT_UNBLOCKED, "unblock history type");
        check(event.previousOperationalStatus().orElseThrow() == AccountOperationalStatus.BLOCKED, "previous state retained");
        check(event.currentOperationalStatus().orElseThrow() == AccountOperationalStatus.ACTIVE, "current state retained");
    }

    private static void blockedAccountCannotExchange() {
        ConsumerRegistry registry = registry();
        Consumer person = registry.register("María Luisa", "Mercader");
        person.getBankAccount().deposit(Currency.SUELDO, 10);
        AccountLifecycleService lifecycle = new AccountLifecycleService(registry);
        lifecycle.process(new AccountLifecycleRequest(
                AccountLifecycleRequestId.generate(), person.getConsumerId(), AccountLifecycleAction.BLOCK, null, "COURT_ORDER"
        ));
        ExchangeOperationService exchange = new ExchangeOperationService(
                registry, new ImplementedExchangePolicy(), new InMemoryTransactionLedger(),
                Clock.fixed(Instant.parse("1456-01-30T10:00:00Z"), ZoneOffset.UTC)
        );
        var result = exchange.exchange(person.getConsumerId(), Currency.SUELDO, Currency.VALERITA, 1);
        check(!result.isAccepted(), "blocked account exchange rejected");
        check(result.getPolicyRejectionReason().orElseThrow() == exchangeCoin.ExchangeRejectionReason.ACCOUNT_NOT_OPERATIONAL,
                "typed operational rejection");
        check(person.getBankAccount().getBalance(Currency.SUELDO) == 10, "balance unchanged");
    }

    private static void closingRequiresZeroBalanceAndReleasesCensus() {
        ConsumerRegistry registry = registry();
        Consumer person = registry.register("Juan-Pablo", "Jornalero");
        person.getBankAccount().deposit(Currency.VALERITA, 3);
        AccountLifecycleService service = new AccountLifecycleService(registry);
        AccountLifecycleResult rejected = service.process(new AccountLifecycleRequest(
                AccountLifecycleRequestId.generate(), person.getConsumerId(), AccountLifecycleAction.CLOSE,
                AccountClosureReason.VOLUNTARY, "VOLUNTARY_CLOSE"
        ));
        check(rejected.getRejectionReason().orElseThrow() == AccountLifecycleRejectionReason.NON_ZERO_BALANCES,
                "non-zero close rejected");
        person.getBankAccount().withdraw(Currency.VALERITA, 3);
        AccountLifecycleResult closed = service.process(new AccountLifecycleRequest(
                AccountLifecycleRequestId.generate(), person.getConsumerId(), AccountLifecycleAction.CLOSE,
                AccountClosureReason.TRANSFERRED_TO_ANOTHER_BANK, "PORTABILITY"
        ));
        check(closed.isCompleted(), "zero-balance account closed");
        check(person.getBankAccount().getOperationalStatus() == AccountOperationalStatus.CLOSED, "closed state");
        check(person.getBankAccount().getHolderStatus() == HolderStatus.PENDING_NEW_HOLDER, "holder detached on close");
        var last = registry.getAccountHistoryJournal().findAll().get(2);
        check(last.type() == AccountHistoryEventType.ACCOUNT_CLOSED, "close event type");
        check(last.status() == AccountHistoryEventStatus.COMPLETED, "close event completed");
        check(last.closureReason().orElseThrow() == AccountClosureReason.TRANSFERRED_TO_ANOTHER_BANK,
                "closure reason retained");

        Consumer replacement = registry.register("Daniel", "Jornalero");
        check(replacement.getBankAccount().getCensusPosition().equals(person.getBankAccount().getCensusPosition()),
                "released profession slot reused");
    }

    private static void pendingHolderCannotBeBlocked() {
        ConsumerRegistry registry = registry();
        Consumer person = registry.register("Lucía", "Comerciante");
        new application.account.AccountHolderService(registry).releaseHolder(person.getConsumerId());
        AccountLifecycleResult result = new AccountLifecycleService(registry).process(new AccountLifecycleRequest(
                AccountLifecycleRequestId.generate(), person.getConsumerId(), AccountLifecycleAction.BLOCK, null, "NO_HOLDER"
        ));
        check(result.getRejectionReason().orElseThrow() == AccountLifecycleRejectionReason.ACCOUNT_PENDING_NEW_HOLDER,
                "pending-holder block rejected");
    }

    private static void closedAccountIsTerminal() {
        ConsumerRegistry registry = registry();
        Consumer person = registry.register("Elena", "Carpintero");
        AccountLifecycleService service = new AccountLifecycleService(registry);
        service.process(new AccountLifecycleRequest(AccountLifecycleRequestId.generate(), person.getConsumerId(),
                AccountLifecycleAction.CLOSE, AccountClosureReason.BANK_DECISION, "TERMINATION"));
        AccountLifecycleResult result = service.process(new AccountLifecycleRequest(
                AccountLifecycleRequestId.generate(), person.getConsumerId(), AccountLifecycleAction.UNBLOCK, null, "INVALID_REOPEN"
        ));
        check(result.getRejectionReason().orElseThrow() == AccountLifecycleRejectionReason.ACCOUNT_ALREADY_CLOSED,
                "closed account remains terminal");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
