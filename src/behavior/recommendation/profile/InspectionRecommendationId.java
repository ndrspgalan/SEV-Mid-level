package behavior.recommendation.profile;

import behavior.health.profile.EconomicHealthAssessmentId;
import java.util.Objects;

public record InspectionRecommendationId(String value) {
    public InspectionRecommendationId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("value");
    }

    public static InspectionRecommendationId from(EconomicHealthAssessmentId assessmentId) {
        return new InspectionRecommendationId("inspection-recommendation:" + Objects.requireNonNull(assessmentId).value());
    }
}
