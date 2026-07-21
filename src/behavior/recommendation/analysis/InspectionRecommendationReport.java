package behavior.recommendation.analysis;

import behavior.recommendation.profile.InspectionRecommendation;
import java.util.*;

public record InspectionRecommendationReport(
        long assessmentsExamined,
        long recommendationsProduced,
        long noActionRecommendations,
        long individualRecommendations,
        long groupRecommendations,
        long professionRecommendations,
        long systemicRecommendations,
        List<InspectionRecommendation> recommendations
) {
    public InspectionRecommendationReport {
        if (assessmentsExamined < 0 || recommendationsProduced < 0 || noActionRecommendations < 0
                || individualRecommendations < 0 || groupRecommendations < 0
                || professionRecommendations < 0 || systemicRecommendations < 0) {
            throw new IllegalArgumentException("negative report value");
        }
        recommendations = List.copyOf(Objects.requireNonNull(recommendations));
    }
}
