package economicEvent.normalization;

import accountHistory.AccountHistoryEvent;
import operationalControl.OperationalDecisionRecord;
import transaction.TransactionRecord;

import java.util.Objects;

/** Delegates a supported Junior record to its canonical economic-event normalizer. */
public final class CompositeEconomicEventNormalizer implements EconomicEventNormalizer<Object> {
    private final TransactionEconomicEventNormalizer transactionNormalizer;
    private final AccountHistoryEconomicEventNormalizer accountHistoryNormalizer;
    private final OperationalDecisionEconomicEventNormalizer operationalDecisionNormalizer;

    public CompositeEconomicEventNormalizer(
            TransactionEconomicEventNormalizer transactionNormalizer,
            AccountHistoryEconomicEventNormalizer accountHistoryNormalizer,
            OperationalDecisionEconomicEventNormalizer operationalDecisionNormalizer) {
        this.transactionNormalizer = Objects.requireNonNull(transactionNormalizer);
        this.accountHistoryNormalizer = Objects.requireNonNull(accountHistoryNormalizer);
        this.operationalDecisionNormalizer = Objects.requireNonNull(operationalDecisionNormalizer);
    }

    @Override
    public EconomicEventNormalizationResult normalize(Object source) {
        Objects.requireNonNull(source, "source must not be null");
        if (source instanceof TransactionRecord record) return transactionNormalizer.normalize(record);
        if (source instanceof AccountHistoryEvent event) return accountHistoryNormalizer.normalize(event);
        if (source instanceof OperationalDecisionRecord decision) return operationalDecisionNormalizer.normalize(decision);
        throw new IllegalArgumentException("unsupported normalization source: " + source.getClass().getName());
    }
}
