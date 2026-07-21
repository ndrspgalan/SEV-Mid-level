package application.analytics.projection;

import accountHistory.AccountHistoryEvent;
import accountHistory.AccountHistoryJournal;
import economicEvent.EconomicEvent;
import economicEvent.normalization.*;
import economicEvent.repository.EconomicEventRepository;
import economicEvent.repository.EconomicEventSaveResult;
import operationalControl.OperationalDecisionJournal;
import operationalControl.OperationalDecisionRecord;
import transaction.TransactionLedger;
import transaction.TransactionRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Explicit, synchronous and idempotent projection from Junior journals into Mid analytics. */
public final class EconomicEventProjectionService {
    private final TransactionLedger transactionLedger;
    private final AccountHistoryJournal accountHistoryJournal;
    private final OperationalDecisionJournal operationalDecisionJournal;
    private final EconomicEventRepository repository;
    private final CompositeEconomicEventNormalizer normalizer;

    public EconomicEventProjectionService(
            TransactionLedger transactionLedger,
            AccountHistoryJournal accountHistoryJournal,
            OperationalDecisionJournal operationalDecisionJournal,
            EconomicEventRepository repository,
            CompositeEconomicEventNormalizer normalizer) {
        this.transactionLedger = Objects.requireNonNull(transactionLedger);
        this.accountHistoryJournal = Objects.requireNonNull(accountHistoryJournal);
        this.operationalDecisionJournal = Objects.requireNonNull(operationalDecisionJournal);
        this.repository = Objects.requireNonNull(repository);
        this.normalizer = Objects.requireNonNull(normalizer);
    }

    public EconomicEventProjectionResult projectTransactions() {
        return project(transactionLedger.findAll());
    }

    public EconomicEventProjectionResult projectAccountHistory() {
        return project(accountHistoryJournal.findAll());
    }

    public EconomicEventProjectionResult projectOperationalDecisions() {
        return project(operationalDecisionJournal.findAll());
    }

    public EconomicEventProjectionResult projectAll() {
        return projectTransactions().plus(projectAccountHistory()).plus(projectOperationalDecisions());
    }

    private EconomicEventProjectionResult project(List<?> sources) {
        int created = 0;
        int alreadyPresent = 0;
        List<EconomicEventProjectionFailure> failures = new ArrayList<>();

        for (Object source : sources) {
            EconomicEventNormalizationResult result = normalizer.normalize(source);
            if (result instanceof EconomicEventNormalizationFailure failure) {
                failures.add(new EconomicEventProjectionFailure(
                        failure.sourceType(), failure.sourceId(), failure.reason(), failure.detail()));
                continue;
            }
            EconomicEventNormalizationSuccess success = (EconomicEventNormalizationSuccess) result;
            try {
                for (EconomicEvent event : success.events()) {
                    EconomicEventSaveResult saveResult = repository.save(event);
                    if (saveResult == EconomicEventSaveResult.CREATED) created++;
                    else alreadyPresent++;
                }
            } catch (RuntimeException exception) {
                failures.add(new EconomicEventProjectionFailure(
                        inferSourceType(source), inferSourceId(source),
                        EconomicEventNormalizationFailureReason.INCONSISTENT_SOURCE_DATA,
                        exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage()));
            }
        }
        return new EconomicEventProjectionResult(sources.size(), created, alreadyPresent, failures);
    }

    private static economicEvent.EconomicEventSourceType inferSourceType(Object source) {
        if (source instanceof TransactionRecord) return economicEvent.EconomicEventSourceType.TRANSACTION_LEDGER;
        if (source instanceof AccountHistoryEvent) return economicEvent.EconomicEventSourceType.ACCOUNT_HISTORY_JOURNAL;
        if (source instanceof OperationalDecisionRecord) return economicEvent.EconomicEventSourceType.OPERATIONAL_DECISION_JOURNAL;
        throw new IllegalArgumentException("unsupported source");
    }

    private static String inferSourceId(Object source) {
        if (source instanceof TransactionRecord record) return record.id().toString();
        if (source instanceof AccountHistoryEvent event) return event.eventId().toString();
        if (source instanceof OperationalDecisionRecord decision)
            return decision.transactionId().value() + "|" + decision.accountId().value() + "|" + decision.operationType();
        return source.getClass().getName();
    }
}
