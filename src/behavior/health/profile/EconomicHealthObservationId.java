package behavior.health.profile;

import java.util.Objects;

public record EconomicHealthObservationId(String value) {
    public EconomicHealthObservationId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("value");
    }
    public static EconomicHealthObservationId of(EconomicHealthAssessmentId assessmentId, int ordinal) {
        if (ordinal < 1) throw new IllegalArgumentException("ordinal");
        return new EconomicHealthObservationId(Objects.requireNonNull(assessmentId).value() + ":observation:" + ordinal);
    }
}
