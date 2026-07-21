package behavior.correlation.profile;

import behavior.alignment.profile.StructuralAlignment;
import behavior.alignment.profile.StructuralAlignmentId;
import behavior.temporal.SeasonPeriod;
import java.util.*;

/** Complete M3.5 descriptive map for one season. It emits no health or inspection conclusion. */
public record EconomicCorrelationGraph(
        EconomicCorrelationGraphId id,
        SeasonPeriod seasonPeriod,
        Map<StructuralAlignmentId, StructuralAlignment> alignments,
        List<AlignmentDisplacement> displacements,
        List<EconomicCorrelation> correlations,
        List<EconomicCorrelationCluster> clusters
) {
    public EconomicCorrelationGraph {
        Objects.requireNonNull(id); Objects.requireNonNull(seasonPeriod);
        if (!id.equals(EconomicCorrelationGraphId.from(seasonPeriod))) throw new IllegalArgumentException("graph identity mismatch");
        alignments = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(alignments)));
        displacements = List.copyOf(Objects.requireNonNull(displacements));
        correlations = List.copyOf(Objects.requireNonNull(correlations));
        clusters = List.copyOf(Objects.requireNonNull(clusters));
    }
}
