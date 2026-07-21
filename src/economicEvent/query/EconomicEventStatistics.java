package economicEvent.query;

import coinProperties.Currency;
import economicEvent.*;

import java.util.*;

/** Descriptive statistics only; no risk or anomaly interpretation. */
public record EconomicEventStatistics(
        long totalEvents,
        Map<EconomicEventType, Long> byType,
        Map<EconomicEventCategory, Long> byCategory,
        Map<EconomicEventStatus, Long> byStatus,
        Map<EconomicEventSourceType, Long> bySource,
        Map<Currency, Long> monetaryVolume,
        long monetaryEvents,
        long rejectedEvents,
        long uniqueActorAccounts,
        long uniqueConsumers
) {
    public EconomicEventStatistics {
        if (totalEvents < 0 || monetaryEvents < 0 || rejectedEvents < 0
                || uniqueActorAccounts < 0 || uniqueConsumers < 0) {
            throw new IllegalArgumentException("statistics counters must not be negative");
        }
        byType = immutable(byType, "byType");
        byCategory = immutable(byCategory, "byCategory");
        byStatus = immutable(byStatus, "byStatus");
        bySource = immutable(bySource, "bySource");
        monetaryVolume = immutable(monetaryVolume, "monetaryVolume");
    }

    public long count(EconomicEventType type) { return byType.getOrDefault(type, 0L); }
    public long count(EconomicEventCategory category) { return byCategory.getOrDefault(category, 0L); }
    public long count(EconomicEventStatus status) { return byStatus.getOrDefault(status, 0L); }
    public long volume(Currency currency) { return monetaryVolume.getOrDefault(currency, 0L); }

    private static <K> Map<K, Long> immutable(Map<K, Long> source, String label) {
        Objects.requireNonNull(source, label + " must not be null");
        LinkedHashMap<K, Long> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            Objects.requireNonNull(key, label + " key must not be null");
            Objects.requireNonNull(value, label + " value must not be null");
            if (value < 0) throw new IllegalArgumentException(label + " values must not be negative");
            copy.put(key, value);
        });
        return Collections.unmodifiableMap(copy);
    }
}
