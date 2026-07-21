package economicEvent.normalization;

import accountHistory.AccountHistoryEvent;
import accountHistory.AccountHistoryEventStatus;
import economicEvent.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Converts immutable account-history records into canonical Mid analytical events. */
public final class AccountHistoryEconomicEventNormalizer
        implements EconomicEventNormalizer<AccountHistoryEvent> {

    @Override
    public EconomicEventNormalizationResult normalize(AccountHistoryEvent record) {
        Objects.requireNonNull(record, "account history event must not be null");
        try {
            EconomicEventType type = mapType(record);
            EconomicEventStatus status = record.status() == AccountHistoryEventStatus.COMPLETED
                    ? EconomicEventStatus.SUCCEEDED
                    : EconomicEventStatus.REJECTED;

            Optional<String> rejectionReason = record.rejectionReason();
            if (status == EconomicEventStatus.REJECTED && rejectionReason.isEmpty()) {
                return failure(record, EconomicEventNormalizationFailureReason.INCONSISTENT_SOURCE_DATA,
                        "rejected account-history event does not preserve a rejection reason");
            }

            EconomicEventSource source = new EconomicEventSource(
                    EconomicEventSourceType.ACCOUNT_HISTORY_JOURNAL,
                    record.eventId().toString(),
                    record.reference());

            EconomicActor actor = new EconomicActor(
                    record.bankAccountId(),
                    record.consumerId(),
                    institutionalContext(record));

            EconomicEvent event = new EconomicEvent(
                    source.eventId(),
                    record.occurredAt(),
                    type,
                    category(type),
                    status,
                    actor,
                    Optional.empty(),
                    Optional.empty(),
                    Optional.empty(),
                    professionContext(record),
                    Optional.empty(),
                    status == EconomicEventStatus.REJECTED ? rejectionReason : Optional.empty(),
                    source,
                    attributes(record));

            return new EconomicEventNormalizationSuccess(event);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(record, EconomicEventNormalizationFailureReason.INCONSISTENT_SOURCE_DATA,
                    exception.getMessage());
        }
    }

    private static EconomicEventType mapType(AccountHistoryEvent record) {
        return switch (record.type()) {
            case ACCOUNT_REGISTERED -> EconomicEventType.ACCOUNT_REGISTERED;
            case PROFESSION_CHANGED -> EconomicEventType.PROFESSION_CHANGED;
            case HOLDER_RELEASED -> EconomicEventType.HOLDER_RELEASED;
            case HOLDER_ASSIGNED -> EconomicEventType.HOLDER_ASSIGNED;
            case ACCOUNT_BLOCKED -> EconomicEventType.ACCOUNT_BLOCKED;
            case ACCOUNT_UNBLOCKED -> EconomicEventType.ACCOUNT_UNBLOCKED;
            case ACCOUNT_CLOSED -> EconomicEventType.ACCOUNT_CLOSED;
        };
    }

    private static EconomicEventCategory category(EconomicEventType type) {
        return switch (type) {
            case ACCOUNT_REGISTERED, PROFESSION_CHANGED, HOLDER_RELEASED, HOLDER_ASSIGNED ->
                    EconomicEventCategory.INSTITUTIONAL;
            case ACCOUNT_BLOCKED, ACCOUNT_UNBLOCKED, ACCOUNT_CLOSED ->
                    EconomicEventCategory.LIFECYCLE;
            default -> throw new IllegalArgumentException("unsupported account-history economic event type: " + type);
        };
    }

    /**
     * A completed record exposes the resulting context. A rejected record retains the context
     * that was actually in force, rather than treating a requested value as an accomplished change.
     */
    private static Optional<banking.identity.Profession> professionContext(AccountHistoryEvent record) {
        if (record.status() == AccountHistoryEventStatus.REJECTED) {
            return record.previousProfession().or(() -> record.currentProfession());
        }
        return record.currentProfession().or(() -> record.previousProfession());
    }

    private static Optional<banking.identity.InstitutionalAccountId> institutionalContext(AccountHistoryEvent record) {
        if (record.status() == AccountHistoryEventStatus.REJECTED) {
            return record.previousInstitutionalId().or(() -> record.currentInstitutionalId());
        }
        return record.currentInstitutionalId().or(() -> record.previousInstitutionalId());
    }

    private static Map<String, String> attributes(AccountHistoryEvent record) {
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("reference", record.reference());
        attributes.put("historyEventStatus", record.status().name());

        record.previousProfession().ifPresent(value -> attributes.put("previousProfession", value.toString()));
        record.currentProfession().ifPresent(value -> attributes.put("currentProfession", value.toString()));
        record.previousInstitutionalId().ifPresent(value -> attributes.put("previousInstitutionalId", value.toString()));
        record.currentInstitutionalId().ifPresent(value -> attributes.put("currentInstitutionalId", value.toString()));
        record.previousHolderStatus().ifPresent(value -> attributes.put("previousHolderStatus", value.name()));
        record.currentHolderStatus().ifPresent(value -> attributes.put("currentHolderStatus", value.name()));
        record.previousOperationalStatus().ifPresent(value -> attributes.put("previousOperationalStatus", value.name()));
        record.currentOperationalStatus().ifPresent(value -> attributes.put("currentOperationalStatus", value.name()));
        record.closureReason().ifPresent(value -> attributes.put("closureReason", value.name()));

        return attributes;
    }

    private static EconomicEventNormalizationFailure failure(AccountHistoryEvent record,
                                                               EconomicEventNormalizationFailureReason reason,
                                                               String detail) {
        return new EconomicEventNormalizationFailure(
                EconomicEventSourceType.ACCOUNT_HISTORY_JOURNAL,
                record.eventId().toString(),
                reason,
                detail == null ? reason.name() : detail);
    }
}
