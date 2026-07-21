package economicEvent;

import banking.identity.Profession;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Canonical analytical representation of an immutable fact produced by SEV Junior.
 * It preserves historical context and provenance, but performs no risk interpretation.
 */
public record EconomicEvent(
        EconomicEventId id,
        Instant occurredAt,
        EconomicEventType type,
        EconomicEventCategory category,
        EconomicEventStatus status,
        EconomicActor actor,
        Optional<EconomicCounterparty> counterparty,
        Optional<EconomicAmount> primaryAmount,
        Optional<EconomicAmount> secondaryAmount,
        Optional<Profession> actorProfession,
        Optional<String> productCategory,
        Optional<String> rejectionReason,
        EconomicEventSource source,
        Map<String, String> attributes
) {
    public EconomicEvent {
        Objects.requireNonNull(id, "economic event id must not be null");
        Objects.requireNonNull(occurredAt, "occurred at must not be null");
        Objects.requireNonNull(type, "economic event type must not be null");
        Objects.requireNonNull(category, "economic event category must not be null");
        Objects.requireNonNull(status, "economic event status must not be null");
        Objects.requireNonNull(actor, "economic event actor must not be null");
        counterparty = requireOptional(counterparty, "counterparty");
        primaryAmount = requireOptional(primaryAmount, "primary amount");
        secondaryAmount = requireOptional(secondaryAmount, "secondary amount");
        actorProfession = requireOptional(actorProfession, "actor profession");
        productCategory = optionalText(productCategory, "product category");
        rejectionReason = optionalText(rejectionReason, "rejection reason");
        Objects.requireNonNull(source, "economic event source must not be null");
        attributes = immutableAttributes(attributes);

        validateIdentity(id, source);
        validateTypeCategory(type, category);
        validateOutcome(status, rejectionReason);
        validateAmounts(type, primaryAmount, secondaryAmount);
        validateCounterparty(type, actor, counterparty);
    }

    public boolean rejected() { return status == EconomicEventStatus.REJECTED; }
    public boolean monetary() { return primaryAmount.isPresent() || secondaryAmount.isPresent(); }

    private static <T> Optional<T> requireOptional(Optional<T> value, String label) {
        return Objects.requireNonNull(value, label + " optional must not be null");
    }

    private static Optional<String> optionalText(Optional<String> value, String label) {
        Objects.requireNonNull(value, label + " optional must not be null");
        return value.map(text -> {
            String normalized = Objects.requireNonNull(text, label + " must not be null").trim();
            if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
            return normalized;
        });
    }

    private static Map<String, String> immutableAttributes(Map<String, String> attributes) {
        Objects.requireNonNull(attributes, "attributes must not be null");
        LinkedHashMap<String, String> copy = new LinkedHashMap<>();
        attributes.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String key = requireText(entry.getKey(), "attribute key");
                    String value = requireText(entry.getValue(), "attribute value");
                    if (copy.putIfAbsent(key, value) != null) {
                        throw new IllegalArgumentException("duplicate normalized attribute key: " + key);
                    }
                });
        return Collections.unmodifiableMap(copy);
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return normalized;
    }

    private static void validateIdentity(EconomicEventId id, EconomicEventSource source) {
        if (!id.equals(source.eventId()) && !id.value().startsWith(source.eventId().value().replace(":PRIMARY", ":"))) {
            throw new IllegalArgumentException("economic event id must be derived from its source");
        }
    }

    private static void validateTypeCategory(EconomicEventType type, EconomicEventCategory category) {
        EconomicEventCategory expected = switch (type) {
            case MONETARY_MINTED, CURRENCY_EXCHANGED -> EconomicEventCategory.MONETARY;
            case PURCHASE_EXECUTED -> EconomicEventCategory.COMMERCIAL;
            case FUNDS_TRANSFERRED -> EconomicEventCategory.TRANSFER;
            case ACCOUNT_REGISTERED, PROFESSION_CHANGED, HOLDER_RELEASED, HOLDER_ASSIGNED -> EconomicEventCategory.INSTITUTIONAL;
            case ACCOUNT_BLOCKED, ACCOUNT_UNBLOCKED, ACCOUNT_CLOSED -> EconomicEventCategory.LIFECYCLE;
            case OPERATION_AUTHORIZED, OPERATION_REJECTED -> EconomicEventCategory.OPERATIONAL_CONTROL;
        };
        if (category != expected) {
            throw new IllegalArgumentException("category " + category + " is incompatible with event type " + type);
        }
    }

    private static void validateOutcome(EconomicEventStatus status, Optional<String> rejectionReason) {
        if (status == EconomicEventStatus.REJECTED && rejectionReason.isEmpty()) {
            throw new IllegalArgumentException("rejected economic events must preserve a rejection reason");
        }
        if (status != EconomicEventStatus.REJECTED && rejectionReason.isPresent()) {
            throw new IllegalArgumentException("only rejected economic events may contain a rejection reason");
        }
    }

    private static void validateAmounts(EconomicEventType type,
                                        Optional<EconomicAmount> primary,
                                        Optional<EconomicAmount> secondary) {
        switch (type) {
            case MONETARY_MINTED, PURCHASE_EXECUTED, FUNDS_TRANSFERRED, OPERATION_AUTHORIZED, OPERATION_REJECTED -> {
                if (primary.isEmpty()) throw new IllegalArgumentException(type + " requires a primary amount");
                if (secondary.isPresent()) throw new IllegalArgumentException(type + " must not contain a secondary amount");
            }
            case CURRENCY_EXCHANGED -> {
                if (primary.isEmpty() || secondary.isEmpty()) {
                    throw new IllegalArgumentException("currency exchange requires primary and secondary amounts");
                }
                if (primary.get().currency() == secondary.get().currency()) {
                    throw new IllegalArgumentException("currency exchange amounts must use different currencies");
                }
            }
            default -> {
                if (primary.isPresent() || secondary.isPresent()) {
                    throw new IllegalArgumentException(type + " must not contain monetary amounts");
                }
            }
        }
    }

    private static void validateCounterparty(EconomicEventType type,
                                             EconomicActor actor,
                                             Optional<EconomicCounterparty> counterparty) {
        boolean required = type == EconomicEventType.PURCHASE_EXECUTED || type == EconomicEventType.FUNDS_TRANSFERRED;
        if (required && counterparty.isEmpty()) {
            throw new IllegalArgumentException(type + " requires a counterparty");
        }
        counterparty.ifPresent(value -> {
            if (actor.accountId().equals(value.accountId())) {
                throw new IllegalArgumentException("actor and counterparty accounts must be different");
            }
        });
    }
}
