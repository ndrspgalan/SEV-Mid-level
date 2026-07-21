package economicEvent.query;

import banking.identity.BankAccountId;
import banking.identity.ConsumerId;
import banking.identity.Profession;
import coinProperties.Currency;
import economicEvent.*;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Immutable analytical filter. All present criteria are combined with AND semantics. */
public record EconomicEventQuery(
        Optional<BankAccountId> actorAccountId,
        Optional<BankAccountId> counterpartyAccountId,
        Optional<ConsumerId> consumerId,
        Optional<Profession> actorProfession,
        Optional<EconomicEventType> type,
        Optional<EconomicEventCategory> category,
        Optional<EconomicEventStatus> status,
        Optional<Currency> currency,
        Optional<Integer> minimumAmountInclusive,
        Optional<Integer> maximumAmountInclusive,
        Optional<Instant> occurredFromInclusive,
        Optional<Instant> occurredToExclusive,
        Optional<EconomicEventSourceType> sourceType,
        Optional<String> sourceId,
        Optional<Boolean> rejected,
        EconomicEventSortDirection sortDirection,
        EconomicEventPageRequest pageRequest
) {
    public EconomicEventQuery {
        actorAccountId = requiredOptional(actorAccountId, "actorAccountId");
        counterpartyAccountId = requiredOptional(counterpartyAccountId, "counterpartyAccountId");
        consumerId = requiredOptional(consumerId, "consumerId");
        actorProfession = requiredOptional(actorProfession, "actorProfession");
        type = requiredOptional(type, "type");
        category = requiredOptional(category, "category");
        status = requiredOptional(status, "status");
        currency = requiredOptional(currency, "currency");
        minimumAmountInclusive = nonNegative(minimumAmountInclusive, "minimumAmountInclusive");
        maximumAmountInclusive = nonNegative(maximumAmountInclusive, "maximumAmountInclusive");
        occurredFromInclusive = requiredOptional(occurredFromInclusive, "occurredFromInclusive");
        occurredToExclusive = requiredOptional(occurredToExclusive, "occurredToExclusive");
        sourceType = requiredOptional(sourceType, "sourceType");
        sourceId = normalizedText(sourceId, "sourceId");
        rejected = requiredOptional(rejected, "rejected");
        sortDirection = Objects.requireNonNull(sortDirection, "sortDirection must not be null");
        pageRequest = Objects.requireNonNull(pageRequest, "pageRequest must not be null");

        if (minimumAmountInclusive.isPresent() && maximumAmountInclusive.isPresent()
                && minimumAmountInclusive.get() > maximumAmountInclusive.get()) {
            throw new IllegalArgumentException("minimumAmountInclusive must not exceed maximumAmountInclusive");
        }
        if (occurredFromInclusive.isPresent() && occurredToExclusive.isPresent()
                && !occurredFromInclusive.get().isBefore(occurredToExclusive.get())) {
            throw new IllegalArgumentException("occurredFromInclusive must be before occurredToExclusive");
        }
        if (status.isPresent() && rejected.isPresent()) {
            boolean statusRejected = status.get() == EconomicEventStatus.REJECTED;
            if (statusRejected != rejected.get()) {
                throw new IllegalArgumentException("status and rejected filters are contradictory");
            }
        }
    }

    public static EconomicEventQuery all(EconomicEventPageRequest pageRequest) {
        return new EconomicEventQuery(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), EconomicEventSortDirection.NEWEST_FIRST, pageRequest);
    }

    private static <T> Optional<T> requiredOptional(Optional<T> value, String label) {
        return Objects.requireNonNull(value, label + " must not be null");
    }

    private static Optional<Integer> nonNegative(Optional<Integer> value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        return value.map(number -> {
            if (number < 0) throw new IllegalArgumentException(label + " must not be negative");
            return number;
        });
    }

    private static Optional<String> normalizedText(Optional<String> value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        return value.map(text -> {
            String normalized = Objects.requireNonNull(text, label + " value must not be null").trim();
            if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
            return normalized;
        });
    }
}
