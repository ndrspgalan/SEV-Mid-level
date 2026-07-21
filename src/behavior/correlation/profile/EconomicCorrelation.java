package behavior.correlation.profile;

import behavior.alignment.profile.StructuralAlignmentId;
import behavior.temporal.SeasonPeriod;
import java.util.*;

/** Explainable relation between two M3.4 alignments in the same season. */
public record EconomicCorrelation(
        EconomicCorrelationId id,
        StructuralAlignmentId firstAlignmentId,
        StructuralAlignmentId secondAlignmentId,
        SeasonPeriod seasonPeriod,
        Set<EconomicCorrelationType> types,
        Set<String> sharedCompatibleProfiles,
        Set<String> sharedDisplacementTargets,
        Set<String> mirrorBridges
) {
    public EconomicCorrelation {
        Objects.requireNonNull(id); Objects.requireNonNull(firstAlignmentId); Objects.requireNonNull(secondAlignmentId);
        Objects.requireNonNull(seasonPeriod);
        if (firstAlignmentId.equals(secondAlignmentId)) throw new IllegalArgumentException("self correlation");
        if (!id.equals(EconomicCorrelationId.between(firstAlignmentId, secondAlignmentId))) throw new IllegalArgumentException("correlation identity mismatch");
        types = immutableEnumSet(types);
        if (types.isEmpty()) throw new IllegalArgumentException("correlation requires at least one type");
        sharedCompatibleProfiles = immutableSorted(sharedCompatibleProfiles);
        sharedDisplacementTargets = immutableSorted(sharedDisplacementTargets);
        mirrorBridges = immutableSorted(mirrorBridges);
    }

    private static Set<EconomicCorrelationType> immutableEnumSet(Set<EconomicCorrelationType> values) {
        Objects.requireNonNull(values);
        return Collections.unmodifiableSet(values.isEmpty() ? EnumSet.noneOf(EconomicCorrelationType.class) : EnumSet.copyOf(values));
    }

    private static Set<String> immutableSorted(Set<String> values) {
        Objects.requireNonNull(values);
        return Collections.unmodifiableSet(new TreeSet<>(values));
    }
}
