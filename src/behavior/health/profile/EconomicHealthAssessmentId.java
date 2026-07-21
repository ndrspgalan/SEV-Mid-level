package behavior.health.profile;

import behavior.correlation.profile.EconomicCorrelationGraphId;
import java.util.Objects;

public record EconomicHealthAssessmentId(String value) {
    public EconomicHealthAssessmentId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("value");
    }
    public static EconomicHealthAssessmentId from(EconomicCorrelationGraphId graphId) {
        return new EconomicHealthAssessmentId("health:" + Objects.requireNonNull(graphId).value());
    }
}
