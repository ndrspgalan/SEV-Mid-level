package application.analytics.projection;

import economicEvent.EconomicEventSourceType;
import economicEvent.normalization.EconomicEventNormalizationFailureReason;

import java.util.Objects;

public record EconomicEventProjectionFailure(
        EconomicEventSourceType sourceType,
        String sourceId,
        EconomicEventNormalizationFailureReason reason,
        String detail) {
    public EconomicEventProjectionFailure {
        Objects.requireNonNull(sourceType);
        Objects.requireNonNull(sourceId);
        Objects.requireNonNull(reason);
        Objects.requireNonNull(detail);
    }
}
