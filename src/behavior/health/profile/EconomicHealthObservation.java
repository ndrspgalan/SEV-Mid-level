package behavior.health.profile;

import java.util.Objects;

public record EconomicHealthObservation(
        EconomicHealthObservationId id,
        EconomicHealthObservationType type,
        ObservationScope scope,
        EconomicHealthStatus indicator,
        EconomicHealthExplanation explanation
) {
    public EconomicHealthObservation {
        Objects.requireNonNull(id); Objects.requireNonNull(type); Objects.requireNonNull(scope);
        Objects.requireNonNull(indicator); Objects.requireNonNull(explanation);
    }
}
