package behavior.profile;

import banking.identity.BankAccountId;
import banking.identity.ConsumerId;
import coinProperties.Currency;
import consumableRegistry.ConsumableCategory;
import economicEvent.EconomicEventType;

import java.time.Instant;
import java.util.*;

/**
 * Immutable, descriptive projection of how one consumer has operated across the observed history.
 * It aggregates facts only: it does not infer frequency, stability, rupture, risk or fraud.
 */
public record BehaviorProfile(
        BehaviorProfileId id,
        ConsumerId consumerId,
        Set<BankAccountId> observedAccountIds,
        Instant firstEventAt,
        Instant lastEventAt,
        long totalEvents,
        long succeededEvents,
        long rejectedEvents,
        Map<EconomicEventType, Long> eventCountByType,
        Map<Currency, Integer> succeededVolumeByCurrency,
        Set<BankAccountId> counterparties,
        Map<String, ConsumableBehaviorProfile> consumables,
        Map<ConsumableCategory, CategoryBehaviorProfile> consumableCategories
) {
    public BehaviorProfile {
        Objects.requireNonNull(id, "profile id must not be null");
        Objects.requireNonNull(consumerId, "consumer id must not be null");
        if (!id.consumerId().equals(consumerId)) throw new IllegalArgumentException("profile id and consumer id differ");
        observedAccountIds = immutableSet(observedAccountIds, "observed accounts");
        Objects.requireNonNull(firstEventAt, "first event must not be null");
        Objects.requireNonNull(lastEventAt, "last event must not be null");
        if (lastEventAt.isBefore(firstEventAt)) throw new IllegalArgumentException("last event precedes first event");
        if (totalEvents <= 0 || succeededEvents < 0 || rejectedEvents < 0 || succeededEvents + rejectedEvents != totalEvents) {
            throw new IllegalArgumentException("invalid event counters");
        }
        eventCountByType = immutableEnumLongMap(eventCountByType, EconomicEventType.class, "event counts");
        long counted = eventCountByType.values().stream().mapToLong(Long::longValue).sum();
        if (counted != totalEvents) throw new IllegalArgumentException("event type counts do not equal total events");
        succeededVolumeByCurrency = immutableEnumIntMap(succeededVolumeByCurrency, Currency.class, "volume");
        counterparties = immutableSet(counterparties, "counterparties");
        consumables = immutableMap(consumables, "consumables");
        consumableCategories = immutableMap(consumableCategories, "consumable categories");
    }

    private static <T> Set<T> immutableSet(Set<T> values, String label) {
        Objects.requireNonNull(values, label + " must not be null");
        LinkedHashSet<T> copy = new LinkedHashSet<>();
        for (T value : values) copy.add(Objects.requireNonNull(value, label + " value must not be null"));
        return Collections.unmodifiableSet(copy);
    }

    private static <K, V> Map<K, V> immutableMap(Map<K, V> values, String label) {
        Objects.requireNonNull(values, label + " must not be null");
        LinkedHashMap<K, V> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> copy.put(Objects.requireNonNull(key), Objects.requireNonNull(value)));
        return Collections.unmodifiableMap(copy);
    }

    private static <E extends Enum<E>> Map<E, Long> immutableEnumLongMap(Map<E, Long> values, Class<E> type, String label) {
        Objects.requireNonNull(values, label + " must not be null");
        EnumMap<E, Long> copy = new EnumMap<>(type);
        values.forEach((key, value) -> {
            if (value == null || value <= 0) throw new IllegalArgumentException(label + " values must be positive");
            copy.put(Objects.requireNonNull(key), value);
        });
        return Collections.unmodifiableMap(copy);
    }

    private static <E extends Enum<E>> Map<E, Integer> immutableEnumIntMap(Map<E, Integer> values, Class<E> type, String label) {
        Objects.requireNonNull(values, label + " must not be null");
        EnumMap<E, Integer> copy = new EnumMap<>(type);
        values.forEach((key, value) -> {
            if (value == null || value < 0) throw new IllegalArgumentException(label + " values must not be negative");
            copy.put(Objects.requireNonNull(key), value);
        });
        return Collections.unmodifiableMap(copy);
    }
}
