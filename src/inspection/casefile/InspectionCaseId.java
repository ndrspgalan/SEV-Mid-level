package inspection.casefile;

import behavior.recommendation.profile.InspectionRecommendationId;
import java.util.Objects;

/** Stable identity of the inspection case opened from one recommendation. */
public record InspectionCaseId(String value) {
    public InspectionCaseId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("value");
    }

    public static InspectionCaseId from(InspectionRecommendationId recommendationId) {
        return new InspectionCaseId("INSPECTION-CASE|" + Objects.requireNonNull(recommendationId).value());
    }
}
