package application.account;

import accountHistory.*;
import banking.identity.HolderStatus;
import consumerRegistry.BankAccount;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;

import java.time.Clock;
import java.util.Objects;

public final class AccountHolderService {
    private final ConsumerRegistry registry;
    private final AccountHistoryJournal historyJournal;
    private final Clock clock;

    public AccountHolderService(ConsumerRegistry registry) {
        this(registry, registry.getAccountHistoryJournal(), registry.getClock());
    }

    public AccountHolderService(ConsumerRegistry registry, AccountHistoryJournal historyJournal, Clock clock) {
        this.registry = Objects.requireNonNull(registry);
        this.historyJournal = Objects.requireNonNull(historyJournal);
        this.clock = Objects.requireNonNull(clock);
    }

    public synchronized AccountReleaseResult releaseHolder(String id) {
        Consumer consumer = registry.findById(id).orElse(null);
        if (consumer == null) return new AccountReleaseResult(false, id, "Account not found");
        BankAccount account = consumer.getBankAccount();
        if (!account.hasZeroBalances()) {
            record(consumer, account, AccountHistoryEventStatus.REJECTED, "NON_ZERO_BALANCES");
            return new AccountReleaseResult(false, account.getInstitutionalAccountId().toString(), "Account balances must be zero before releasing its holder");
        }
        String previousId = account.getInstitutionalAccountId().toString();
        HolderStatus previousStatus = account.getHolderStatus();
        registry.getProfessionCensus().release(account.getProfession(), account.getCensusPosition(), account.getBankAccountId());
        account.releaseHolder();
        historyJournal.append(new AccountHistoryEvent(
                AccountHistoryEventId.generate(), account.getBankAccountId(), consumer.getStableConsumerId(),
                AccountHistoryEventType.HOLDER_RELEASED, AccountHistoryEventStatus.COMPLETED, clock.instant(),
                account.getProfession(), account.getProfession(), account.getInstitutionalAccountId(), account.getInstitutionalAccountId(),
                previousStatus, account.getHolderStatus(), null, "HOLDER_RELEASE"
        ));
        return new AccountReleaseResult(true, previousId, "Account is pending a new holder");
    }

    private void record(Consumer consumer, BankAccount account, AccountHistoryEventStatus status, String reason) {
        historyJournal.append(new AccountHistoryEvent(
                AccountHistoryEventId.generate(), account.getBankAccountId(), consumer.getStableConsumerId(),
                AccountHistoryEventType.HOLDER_RELEASED, status, clock.instant(),
                account.getProfession(), account.getProfession(), account.getInstitutionalAccountId(), account.getInstitutionalAccountId(),
                account.getHolderStatus(), account.getHolderStatus(), reason, "HOLDER_RELEASE"
        ));
    }
}
