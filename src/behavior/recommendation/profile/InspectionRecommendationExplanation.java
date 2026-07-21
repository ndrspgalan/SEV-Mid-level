package behavior.recommendation.profile;

import behavior.alignment.profile.StructuralAlignmentId;
import behavior.correlation.profile.EconomicCorrelationId;
import behavior.health.profile.EconomicHealthObservationId;
import java.util.*;

/** Immutable trace from an institutional recommendation back to M3.6 and its evidence. */
public record InspectionRecommendationExplanation(
        String summary,
        Set<EconomicHealthObservationId> observationEvidence,
        Set<StructuralAlignmentId> alignmentEvidence,
        Set<EconomicCorrelationId> correlationEvidence,
        Set<String> professions,
        Set<String> institutionalProfiles
) {
    public InspectionRecommendationExplanation {
        if (summary == null || summary.isBlank()) throw new IllegalArgumentException("summary");
        observationEvidence = immutableLinked(observationEvidence);
        alignmentEvidence = immutableLinked(alignmentEvidence);
        correlationEvidence = immutableLinked(correlationEvidence);
        professions = immutableSorted(professions);
        institutionalProfiles = immutableSorted(institutionalProfiles);
    }

    private static <T> Set<T> immutableLinked(Set<T> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(values)));
    }

    private static Set<String> immutableSorted(Set<String> values) {
        return Collections.unmodifiableSet(new TreeSet<>(Objects.requireNonNull(values)));
    }
}
