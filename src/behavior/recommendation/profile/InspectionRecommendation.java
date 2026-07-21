package behavior.recommendation.profile;

import behavior.health.profile.EconomicHealthAssessmentId;
import behavior.temporal.SeasonPeriod;
import java.util.*;

/** M3.7 institutional recommendation. It does not create or execute an inspection. */
public record InspectionRecommendation(
        InspectionRecommendationId id,
        EconomicHealthAssessmentId sourceAssessmentId,
        SeasonPeriod seasonPeriod,
        InspectionRecommendationType type,
        Set<InspectionRecommendationReason> reasons,
        InspectionRecommendationExplanation explanation
) {
    public InspectionRecommendation {
        Objects.requireNonNull(id); Objects.requireNonNull(sourceAssessmentId); Objects.requireNonNull(seasonPeriod);
        Objects.requireNonNull(type); Objects.requireNonNull(explanation);
        if (!id.equals(InspectionRecommendationId.from(sourceAssessmentId))) {
            throw new IllegalArgumentException("recommendation identity mismatch");
        }
        EnumSet<InspectionRecommendationReason> copy = reasons == null || reasons.isEmpty()
                ? EnumSet.noneOf(InspectionRecommendationReason.class)
                : EnumSet.copyOf(reasons);
        if (copy.isEmpty()) throw new IllegalArgumentException("recommendation without reasons");
        reasons = Collections.unmodifiableSet(copy);
        if (type == InspectionRecommendationType.NONE && copy.stream().anyMatch(InspectionRecommendation::isActionable)) {
            throw new IllegalArgumentException("NONE cannot contain actionable reasons");
        }
        if (type != InspectionRecommendationType.NONE && copy.stream().noneMatch(InspectionRecommendation::isActionable)) {
            throw new IllegalArgumentException("actionable recommendation without actionable reason");
        }
    }

    public boolean recommendsInspection() {
        return type != InspectionRecommendationType.NONE;
    }

    private static boolean isActionable(InspectionRecommendationReason reason) {
        return reason != InspectionRecommendationReason.STABLE_ECONOMY
                && reason != InspectionRecommendationReason.INSUFFICIENT_EVIDENCE;
    }
}
