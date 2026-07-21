package application.lifecycle;

import accountHistory.*;
import banking.identity.HolderStatus;
import banking.lifecycle.AccountLifecycleAction;
import banking.lifecycle.AccountLifecycleRequest;
import banking.lifecycle.AccountOperationalStatus;
import consumerRegistry.BankAccount;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class AccountLifecycleService {
    private final ConsumerRegistry registry;
    private final AccountHistoryJournal historyJournal;
    private final AccountLifecycleRequestRegistry requestRegistry;
    private final Clock clock;

    public AccountLifecycleService(ConsumerRegistry registry) {
        this(registry, registry.getAccountHistoryJournal(), new InMemoryAccountLifecycleRequestRegistry(), registry.getClock());
    }

    public AccountLifecycleService(ConsumerRegistry registry, AccountHistoryJournal historyJournal,
                                   AccountLifecycleRequestRegistry requestRegistry, Clock clock) {
        this.registry = Objects.requireNonNull(registry);
        this.historyJournal = Objects.requireNonNull(historyJournal);
        this.requestRegistry = Objects.requireNonNull(requestRegistry);
        this.clock = Objects.requireNonNull(clock);
    }

    public AccountLifecycleResult process(AccountLifecycleRequest request) {
        Objects.requireNonNull(request);
        synchronized (requestRegistry) {
            Optional<ProcessedAccountLifecycleRequest> prior = requestRegistry.find(request.requestId());
            if (prior.isPresent()) {
                if (!prior.orElseThrow().request().equals(request)) {
                    Instant at = clock.instant();
                    return AccountLifecycleResult.rejected(request.requestId(), at, null, null,
                            AccountLifecycleRejectionReason.IDEMPOTENCY_CONFLICT);
                }
                return prior.orElseThrow().result().asReplay();
            }
            AccountLifecycleResult result = execute(request);
            requestRegistry.register(new ProcessedAccountLifecycleRequest(request, result));
            return result;
        }
    }

    private AccountLifecycleResult execute(AccountLifecycleRequest request) {
        Instant occurredAt = clock.instant();
        Consumer consumer = registry.findById(request.accountOrConsumerId()).orElse(null);
        if (consumer == null) {
            return AccountLifecycleResult.rejected(request.requestId(), occurredAt, null, null,
                    AccountLifecycleRejectionReason.ACCOUNT_NOT_FOUND);
        }
        BankAccount account = consumer.getBankAccount();
        AccountOperationalStatus previous = account.getOperationalStatus();
        HolderStatus previousHolderStatus = account.getHolderStatus();

        AccountLifecycleRejectionReason rejection = validate(account, request.action());
        if (rejection != null) {
            record(consumer, account, request, occurredAt, previous, previous,
                    previousHolderStatus, previousHolderStatus, AccountHistoryEventStatus.REJECTED, rejection.name());
            return AccountLifecycleResult.rejected(request.requestId(), occurredAt,
                    account.getBankAccountId().toString(), previous, rejection);
        }

        switch (request.action()) {
            case BLOCK -> account.block();
            case UNBLOCK -> account.unblock();
            case CLOSE -> close(account);
        }
        AccountOperationalStatus current = account.getOperationalStatus();
        record(consumer, account, request, occurredAt, previous, current,
                previousHolderStatus, account.getHolderStatus(), AccountHistoryEventStatus.COMPLETED, null);
        return AccountLifecycleResult.completed(request.requestId(), occurredAt,
                account.getBankAccountId().toString(), previous, current);
    }

    private AccountLifecycleRejectionReason validate(BankAccount account, AccountLifecycleAction action) {
        AccountOperationalStatus status = account.getOperationalStatus();
        if (status == AccountOperationalStatus.CLOSED) return AccountLifecycleRejectionReason.ACCOUNT_ALREADY_CLOSED;
        if (action != AccountLifecycleAction.CLOSE && account.getHolderStatus() != HolderStatus.ASSIGNED) {
            return AccountLifecycleRejectionReason.ACCOUNT_PENDING_NEW_HOLDER;
        }
        return switch (action) {
            case BLOCK -> status == AccountOperationalStatus.BLOCKED
                    ? AccountLifecycleRejectionReason.ALREADY_BLOCKED : null;
            case UNBLOCK -> status != AccountOperationalStatus.BLOCKED
                    ? AccountLifecycleRejectionReason.ACCOUNT_NOT_BLOCKED : null;
            case CLOSE -> account.hasZeroBalances() ? null : AccountLifecycleRejectionReason.NON_ZERO_BALANCES;
        };
    }

    private void close(BankAccount account) {
        if (account.getHolderStatus() == HolderStatus.ASSIGNED) {
            registry.getProfessionCensus().release(account.getProfession(), account.getCensusPosition(), account.getBankAccountId());
        }
        account.close();
    }

    private void record(Consumer consumer, BankAccount account, AccountLifecycleRequest request, Instant at,
                        AccountOperationalStatus previous, AccountOperationalStatus current,
                        HolderStatus previousHolderStatus, HolderStatus currentHolderStatus,
                        AccountHistoryEventStatus eventStatus, String rejectionReason) {
        AccountHistoryEventType type = switch (request.action()) {
            case BLOCK -> AccountHistoryEventType.ACCOUNT_BLOCKED;
            case UNBLOCK -> AccountHistoryEventType.ACCOUNT_UNBLOCKED;
            case CLOSE -> AccountHistoryEventType.ACCOUNT_CLOSED;
        };
        historyJournal.append(new AccountHistoryEvent(
                AccountHistoryEventId.generate(), account.getBankAccountId(), consumer.getStableConsumerId(),
                type, eventStatus, at,
                account.getProfession(), account.getProfession(),
                account.getInstitutionalAccountId(), account.getInstitutionalAccountId(),
                previousHolderStatus, currentHolderStatus,
                previous, current, request.closureReason(), rejectionReason, request.reference()
        ));
    }
}
