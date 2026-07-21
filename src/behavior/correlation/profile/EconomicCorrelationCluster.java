package behavior.correlation.profile;

import behavior.alignment.profile.StructuralAlignmentId;
import java.util.*;

/** Connected component of the descriptive correlation graph. */
public record EconomicCorrelationCluster(
        EconomicCorrelationClusterId id,
        Set<StructuralAlignmentId> alignmentIds,
        Set<EconomicCorrelationId> correlationIds
) {
    public EconomicCorrelationCluster {
        Objects.requireNonNull(id);
        alignmentIds = Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(alignmentIds)));
        correlationIds = Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(correlationIds)));
        if (alignmentIds.isEmpty()) throw new IllegalArgumentException("cluster without alignments");
    }
}
