package behavior.health.profile;

import behavior.correlation.profile.EconomicCorrelationGraphId;
import behavior.temporal.SeasonPeriod;
import java.util.*;

/** M3.6 interpretation of one seasonal correlation graph. It cannot open an inspection. */
public record EconomicHealthAssessment(
        EconomicHealthAssessmentId id,
        EconomicCorrelationGraphId sourceGraphId,
        SeasonPeriod seasonPeriod,
        EconomicHealthStatus status,
        List<EconomicHealthObservation> observations
) {
    public EconomicHealthAssessment {
        Objects.requireNonNull(id); Objects.requireNonNull(sourceGraphId); Objects.requireNonNull(seasonPeriod);
        Objects.requireNonNull(status);
        if (!id.equals(EconomicHealthAssessmentId.from(sourceGraphId))) throw new IllegalArgumentException("assessment identity mismatch");
        observations = List.copyOf(Objects.requireNonNull(observations));
        if (observations.isEmpty()) throw new IllegalArgumentException("assessment without observations");
    }
}
