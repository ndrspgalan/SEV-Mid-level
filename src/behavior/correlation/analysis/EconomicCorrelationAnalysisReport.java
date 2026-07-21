package behavior.correlation.analysis;

import behavior.correlation.profile.EconomicCorrelationGraph;
import java.util.*;

public record EconomicCorrelationAnalysisReport(
        long alignmentsExamined,
        long seasonsExamined,
        long pairComparisons,
        long correlationsProduced,
        long mirrorCorrelations,
        long clustersProduced,
        List<EconomicCorrelationGraph> graphs
) {
    public EconomicCorrelationAnalysisReport {
        if (alignmentsExamined < 0 || seasonsExamined < 0 || pairComparisons < 0 || correlationsProduced < 0 || mirrorCorrelations < 0 || clustersProduced < 0) {
            throw new IllegalArgumentException("negative report value");
        }
        graphs = List.copyOf(Objects.requireNonNull(graphs));
    }
}
