package inspection.jurisprudence.casefile;

import behavior.recommendation.profile.*;
import java.util.*;

/**
 * Deterministic financial identity of a family of univocally similar inspection cases.
 * Season is deliberately excluded so the same identity can later be compared inter-season.
 */
public record JurisprudentialSimilarityKey(
        InspectionRecommendationType origin,
        Set<InspectionRecommendationReason> financialReasons,
        Set<String> professions,
        Set<String> institutionalProfiles
) {
    public JurisprudentialSimilarityKey {
        Objects.requireNonNull(origin);
        if (origin == InspectionRecommendationType.NONE) {
            throw new IllegalArgumentException("NONE has no jurisprudential identity");
        }
        EnumSet<InspectionRecommendationReason> reasonCopy = financialReasons == null || financialReasons.isEmpty()
                ? EnumSet.noneOf(InspectionRecommendationReason.class)
                : EnumSet.copyOf(financialReasons);
        if (reasonCopy.isEmpty()) throw new IllegalArgumentException("jurisprudential identity without reasons");
        financialReasons = Collections.unmodifiableSet(reasonCopy);
        professions = immutableSorted(professions);
        institutionalProfiles = immutableSorted(institutionalProfiles);
    }

    public static JurisprudentialSimilarityKey from(InspectionRecommendation recommendation) {
        Objects.requireNonNull(recommendation);
        return new JurisprudentialSimilarityKey(
                recommendation.type(),
                recommendation.reasons(),
                recommendation.explanation().professions(),
                recommendation.explanation().institutionalProfiles());
    }

    private static Set<String> immutableSorted(Set<String> values) {
        return Collections.unmodifiableSet(new TreeSet<>(Objects.requireNonNull(values)));
    }
}
