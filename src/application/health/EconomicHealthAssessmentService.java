package application.health;

import behavior.correlation.repository.EconomicCorrelationGraphRepository;
import behavior.health.analysis.*;
import behavior.health.profile.*;
import behavior.health.repository.EconomicHealthAssessmentRepository;
import java.util.*;

/** Rebuilds M3.6 interpretations from immutable M3.5 seasonal graphs. */
public final class EconomicHealthAssessmentService {
    private final EconomicCorrelationGraphRepository graphs;
    private final EconomicHealthAssessmentRepository assessments;
    private final EconomicHealthAssessmentAnalyzer analyzer;
    private EconomicHealthAssessmentReport lastReport = new EconomicHealthAssessmentReport(0,0,0,0,0,0,List.of());
    public EconomicHealthAssessmentService(EconomicCorrelationGraphRepository graphs, EconomicHealthAssessmentRepository assessments, EconomicHealthAssessmentAnalyzer analyzer) {
        this.graphs = Objects.requireNonNull(graphs); this.assessments = Objects.requireNonNull(assessments); this.analyzer = Objects.requireNonNull(analyzer);
    }
    public EconomicHealthAssessmentReport rebuild() {
        lastReport = analyzer.analyze(graphs.findAll());
        assessments.replaceAll(lastReport.assessments());
        return lastReport;
    }
    public EconomicHealthAssessmentReport lastReport() { return lastReport; }
    public Optional<EconomicHealthAssessment> findById(EconomicHealthAssessmentId id) { return assessments.findById(id); }
    public List<EconomicHealthAssessment> findAll() { return assessments.findAll(); }
    public long count() { return assessments.count(); }
}
