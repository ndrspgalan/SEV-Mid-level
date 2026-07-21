package behavior.health.profile;

import behavior.alignment.profile.StructuralAlignmentId;
import behavior.correlation.profile.EconomicCorrelationId;
import java.util.*;

/** Traceable explanation for one interpretation. */
public record EconomicHealthExplanation(
        String summary,
        Set<StructuralAlignmentId> alignmentEvidence,
        Set<EconomicCorrelationId> correlationEvidence,
        Set<String> professions,
        Set<String> institutionalProfiles
) {
    public EconomicHealthExplanation {
        if (summary == null || summary.isBlank()) throw new IllegalArgumentException("summary");
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
