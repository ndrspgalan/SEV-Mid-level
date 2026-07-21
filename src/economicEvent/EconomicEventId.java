package economicEvent;

import java.util.Locale;
import java.util.Objects;

/** Stable, deterministic identifier of an analytical economic event. */
public record EconomicEventId(String value) implements Comparable<EconomicEventId> {
    public EconomicEventId {
        Objects.requireNonNull(value, "economic event id must not be null");
        value = value.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("economic event id must not be blank");
        if (value.length() > 240) throw new IllegalArgumentException("economic event id must not exceed 240 characters");
    }

    public static EconomicEventId fromSource(EconomicEventSourceType sourceType, String sourceId) {
        return fromSource(sourceType, sourceId, "PRIMARY");
    }

    public static EconomicEventId fromSource(EconomicEventSourceType sourceType, String sourceId, String variant) {
        Objects.requireNonNull(sourceType, "source type must not be null");
        return new EconomicEventId(sourceType.name() + ":" + normalize(sourceId, "source id") + ":" + normalize(variant, "variant"));
    }

    private static String normalize(String value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        String normalized = value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        if (normalized.contains(":")) throw new IllegalArgumentException(label + " must not contain ':'");
        return normalized;
    }

    @Override public int compareTo(EconomicEventId other) { return value.compareTo(other.value); }
    @Override public String toString() { return value; }
}
