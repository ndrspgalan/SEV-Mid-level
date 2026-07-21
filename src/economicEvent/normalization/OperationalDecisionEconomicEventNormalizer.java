package economicEvent.normalization;

import economicEvent.*;
import operationalControl.OperationalDecisionRecord;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Converts enriched operational-control decisions into canonical Mid analytical events. */
public final class OperationalDecisionEconomicEventNormalizer
        implements EconomicEventNormalizer<OperationalDecisionRecord> {

    @Override
    public EconomicEventNormalizationResult normalize(OperationalDecisionRecord record) {
        Objects.requireNonNull(record, "operational decision record must not be null");
        if (!record.enriched()) {
            return failure(record, EconomicEventNormalizationFailureReason.MISSING_ACTOR,
                    "operational decision does not preserve stable consumer, institutional identity and profession");
        }
        if (record.snapshot().allowed() && record.snapshot().rejectionReason().isPresent()) {
            return failure(record, EconomicEventNormalizationFailureReason.INCONSISTENT_SOURCE_DATA,
                    "allowed operational decision contains a rejection reason");
        }
        if (!record.snapshot().allowed() && record.snapshot().rejectionReason().isEmpty()) {
            return failure(record, EconomicEventNormalizationFailureReason.INCONSISTENT_SOURCE_DATA,
                    "rejected operational decision does not preserve a rejection reason");
        }
        try {
            EconomicEventSource source = new EconomicEventSource(
                    EconomicEventSourceType.OPERATIONAL_DECISION_JOURNAL,
                    record.sourceId(),
                    record.operationType().name());
            EconomicActor actor = new EconomicActor(record.accountId(), record.consumerId().orElseThrow(),
                    record.institutionalAccountId());
            Optional<EconomicCounterparty> counterparty = record.counterpartyAccountId().map(accountId ->
                    new EconomicCounterparty(accountId, record.counterpartyConsumerId(),
                            record.counterpartyInstitutionalAccountId()));
            EconomicEventStatus status = record.snapshot().allowed()
                    ? EconomicEventStatus.SUCCEEDED : EconomicEventStatus.REJECTED;
            EconomicEventType type = record.snapshot().allowed()
                    ? EconomicEventType.OPERATION_AUTHORIZED : EconomicEventType.OPERATION_REJECTED;
            Optional<String> rejectionReason = record.snapshot().rejectionReason().map(Enum::name);

            Map<String, String> attributes = new LinkedHashMap<>();
            attributes.put("operationType", record.operationType().name());
            attributes.put("usageAmountBefore", Long.toString(record.snapshot().usageAmountBefore()));
            attributes.put("usageCountBefore", Integer.toString(record.snapshot().usageCountBefore()));
            attributes.put("projectedAmount", Long.toString(record.snapshot().projectedAmount()));
            attributes.put("projectedCount", Integer.toString(record.snapshot().projectedCount()));
            attributes.put("appliedPolicyIds", record.snapshot().appliedPolicyIds().isEmpty()
                    ? "NONE"
                    : record.snapshot().appliedPolicyIds().stream().map(Object::toString).collect(Collectors.joining(",")));
            record.targetCurrency().ifPresent(value -> attributes.put("targetCurrency", value.name()));
            record.consumableType().ifPresent(value -> attributes.put("consumableType", value.name()));

            EconomicEvent event = new EconomicEvent(
                    source.eventId(),
                    record.recordedAt(),
                    type,
                    EconomicEventCategory.OPERATIONAL_CONTROL,
                    status,
                    actor,
                    counterparty,
                    Optional.of(new EconomicAmount(record.currency(), record.amount())),
                    Optional.empty(),
                    record.profession(),
                    record.consumableType().map(Enum::name),
                    rejectionReason,
                    source,
                    attributes);
            return new EconomicEventNormalizationSuccess(event);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(record, EconomicEventNormalizationFailureReason.INCONSISTENT_SOURCE_DATA,
                    exception.getMessage());
        }
    }

    private static EconomicEventNormalizationFailure failure(OperationalDecisionRecord record,
                                                               EconomicEventNormalizationFailureReason reason,
                                                               String detail) {
        return new EconomicEventNormalizationFailure(
                EconomicEventSourceType.OPERATIONAL_DECISION_JOURNAL,
                record.sourceId(),
                reason,
                detail == null ? reason.name() : detail);
    }
}
