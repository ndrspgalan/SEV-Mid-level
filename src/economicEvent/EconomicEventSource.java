package economicEvent;

import java.util.Objects;
import java.util.Optional;

/** Immutable provenance link to the original operational record. */
public record EconomicEventSource(
        EconomicEventSourceType type,
        String sourceId,
        Optional<String> sourceReference
) {
    public EconomicEventSource {
        Objects.requireNonNull(type, "source type must not be null");
        sourceId = requireText(sourceId, "source id");
        sourceReference = immutableOptionalText(sourceReference, "source reference");
    }

    public EconomicEventSource(EconomicEventSourceType type, String sourceId) {
        this(type, sourceId, Optional.empty());
    }

    public EconomicEventSource(EconomicEventSourceType type, String sourceId, String sourceReference) {
        this(type, sourceId, Optional.of(requireText(sourceReference, "source reference")));
    }

    public EconomicEventId eventId() { return EconomicEventId.fromSource(type, sourceId); }
    public EconomicEventId eventId(String variant) { return EconomicEventId.fromSource(type, sourceId, variant); }

    private static Optional<String> immutableOptionalText(Optional<String> value, String label) {
        Objects.requireNonNull(value, label + " optional must not be null");
        return value.map(text -> requireText(text, label));
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return normalized;
    }
}
