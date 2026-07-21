package economicEvent.normalization;

import economicEvent.EconomicEventSourceType;

import java.util.Objects;

public record EconomicEventNormalizationFailure(
        EconomicEventSourceType sourceType,
        String sourceId,
        EconomicEventNormalizationFailureReason reason,
        String detail
) implements EconomicEventNormalizationResult {
    public EconomicEventNormalizationFailure {
        Objects.requireNonNull(sourceType, "source type must not be null");
        sourceId = requireText(sourceId, "source id");
        Objects.requireNonNull(reason, "failure reason must not be null");
        detail = requireText(detail, "failure detail");
    }
    @Override public boolean successful() { return false; }
    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return normalized;
    }
}
