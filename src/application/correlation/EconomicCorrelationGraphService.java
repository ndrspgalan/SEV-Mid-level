package application.correlation;

import behavior.alignment.repository.StructuralAlignmentRepository;
import behavior.correlation.analysis.*;
import behavior.correlation.profile.*;
import behavior.correlation.repository.EconomicCorrelationGraphRepository;
import operationalControl.profile.ValerianProfessionCreditProfiles;
import java.util.*;

/** Rebuilds M3.5 descriptive graphs from immutable M3.4 alignments. */
public final class EconomicCorrelationGraphService {
    private final StructuralAlignmentRepository alignments;
    private final EconomicCorrelationGraphRepository graphs;
    private final EconomicCorrelationAnalyzer analyzer;
    private EconomicCorrelationAnalysisReport lastReport =
            new EconomicCorrelationAnalysisReport(0, 0, 0, 0, 0, 0, List.of());

    public EconomicCorrelationGraphService(
            StructuralAlignmentRepository alignments,
            EconomicCorrelationGraphRepository graphs,
            EconomicCorrelationAnalyzer analyzer
    ) {
        this.alignments = Objects.requireNonNull(alignments);
        this.graphs = Objects.requireNonNull(graphs);
        this.analyzer = Objects.requireNonNull(analyzer);
    }

    public EconomicCorrelationAnalysisReport rebuild() {
        lastReport = analyzer.analyze(alignments.findAll(), ValerianProfessionCreditProfiles.all());
        graphs.replaceAll(lastReport.graphs());
        return lastReport;
    }

    public EconomicCorrelationAnalysisReport lastReport() { return lastReport; }
    public Optional<EconomicCorrelationGraph> findById(EconomicCorrelationGraphId id) { return graphs.findById(id); }
    public List<EconomicCorrelationGraph> findAll() { return graphs.findAll(); }
    public long count() { return graphs.count(); }
}
