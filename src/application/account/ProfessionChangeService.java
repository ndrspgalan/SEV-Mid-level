package application.account;

import accountHistory.*;
import banking.census.ProfessionCensusSlot;
import banking.identity.HolderStatus;
import banking.identity.InstitutionalAccountId;
import banking.identity.Profession;
import consumerRegistry.BankAccount;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;

import java.time.Clock;
import java.util.Objects;

public final class ProfessionChangeService {
    private final ConsumerRegistry registry;
    private final AccountHistoryJournal historyJournal;
    private final Clock clock;

    public ProfessionChangeService(ConsumerRegistry registry) {
        this(registry, registry.getAccountHistoryJournal(), registry.getClock());
    }

    public ProfessionChangeService(ConsumerRegistry registry, AccountHistoryJournal historyJournal, Clock clock) {
        this.registry = Objects.requireNonNull(registry);
        this.historyJournal = Objects.requireNonNull(historyJournal);
        this.clock = Objects.requireNonNull(clock);
    }

    public synchronized ProfessionChangeResult change(String accountOrConsumerId, String newProfessionName) {
        Consumer consumer = registry.findById(accountOrConsumerId).orElse(null);
        if (consumer == null) {
            return ProfessionChangeResult.rejected(ProfessionChangeRejectionReason.ACCOUNT_NOT_FOUND, "Account not found");
        }
        BankAccount account = consumer.getBankAccount();
        Profession requestedProfession = registry.getProfessionCatalog().find(newProfessionName).orElse(null);

        if (account.getHolderStatus() != HolderStatus.ASSIGNED) {
            recordRejected(consumer, account, requestedProfession, ProfessionChangeRejectionReason.ACCOUNT_PENDING_NEW_HOLDER);
            return ProfessionChangeResult.rejected(ProfessionChangeRejectionReason.ACCOUNT_PENDING_NEW_HOLDER, "Account is pending a new holder");
        }
        if (!account.isOperational()) {
            recordRejected(consumer, account, requestedProfession, ProfessionChangeRejectionReason.ACCOUNT_NOT_OPERATIONAL);
            return ProfessionChangeResult.rejected(ProfessionChangeRejectionReason.ACCOUNT_NOT_OPERATIONAL, "Account is not operational");
        }
        if (requestedProfession == null) {
            recordRejected(consumer, account, null, ProfessionChangeRejectionReason.PROFESSION_NOT_ACCEPTED);
            return ProfessionChangeResult.rejected(ProfessionChangeRejectionReason.PROFESSION_NOT_ACCEPTED, "Profession is not accepted by the bank");
        }
        Profession previousProfession = account.getProfession();
        if (previousProfession.equals(requestedProfession)) {
            recordRejected(consumer, account, requestedProfession, ProfessionChangeRejectionReason.SAME_PROFESSION);
            return ProfessionChangeResult.rejected(ProfessionChangeRejectionReason.SAME_PROFESSION, "The account already uses that profession");
        }

        ProfessionCensusSlot newSlot;
        try {
            newSlot = registry.getProfessionCensus().allocate(requestedProfession, account.getBankAccountId());
        } catch (IllegalStateException ex) {
            recordRejected(consumer, account, requestedProfession, ProfessionChangeRejectionReason.CENSUS_SATURATED);
            return ProfessionChangeResult.rejected(ProfessionChangeRejectionReason.CENSUS_SATURATED, ex.getMessage());
        }

        InstitutionalAccountId previousId = account.getInstitutionalAccountId();
        InstitutionalAccountId newId = InstitutionalAccountId.compose(consumer.getPersonName(), requestedProfession, newSlot.position(), newSlot.reuseSequence());
        try {
            registry.getProfessionCensus().release(previousProfession, account.getCensusPosition(), account.getBankAccountId());
            account.changeInstitutionalIdentity(newId, requestedProfession, newSlot.position(), newSlot.reuseSequence());
            registry.reindexInstitutionalId(consumer, previousId.toString());
            historyJournal.append(new AccountHistoryEvent(
                    AccountHistoryEventId.generate(), account.getBankAccountId(), consumer.getStableConsumerId(),
                    AccountHistoryEventType.PROFESSION_CHANGED, AccountHistoryEventStatus.COMPLETED, clock.instant(),
                    previousProfession, requestedProfession, previousId, newId,
                    HolderStatus.ASSIGNED, HolderStatus.ASSIGNED, null, "PROFESSION_CHANGE"
            ));
            return ProfessionChangeResult.completed(previousId, newId, previousProfession, requestedProfession);
        } catch (RuntimeException failure) {
            try { registry.getProfessionCensus().release(requestedProfession, newSlot.position(), account.getBankAccountId()); }
            catch (RuntimeException ignored) { }
            throw failure;
        }
    }

    private void recordRejected(Consumer consumer, BankAccount account, Profession requested,
                                ProfessionChangeRejectionReason reason) {
        historyJournal.append(new AccountHistoryEvent(
                AccountHistoryEventId.generate(), account.getBankAccountId(), consumer.getStableConsumerId(),
                AccountHistoryEventType.PROFESSION_CHANGED, AccountHistoryEventStatus.REJECTED, clock.instant(),
                account.getProfession(), requested, account.getInstitutionalAccountId(), account.getInstitutionalAccountId(),
                account.getHolderStatus(), account.getHolderStatus(), reason.name(), "PROFESSION_CHANGE"
        ));
    }
}
