package application.recommendation;

import behavior.health.repository.EconomicHealthAssessmentRepository;
import behavior.recommendation.analysis.*;
import behavior.recommendation.profile.*;
import behavior.recommendation.repository.InspectionRecommendationRepository;
import java.util.*;

/** Rebuilds M3.7 recommendations from immutable M3.6 assessments. */
public final class InspectionRecommendationService {
    private final EconomicHealthAssessmentRepository assessments;
    private final InspectionRecommendationRepository recommendations;
    private final InspectionRecommendationAnalyzer analyzer;
    private InspectionRecommendationReport lastReport = new InspectionRecommendationReport(0,0,0,0,0,0,0,List.of());

    public InspectionRecommendationService(
            EconomicHealthAssessmentRepository assessments,
            InspectionRecommendationRepository recommendations,
            InspectionRecommendationAnalyzer analyzer
    ) {
        this.assessments = Objects.requireNonNull(assessments);
        this.recommendations = Objects.requireNonNull(recommendations);
        this.analyzer = Objects.requireNonNull(analyzer);
    }

    public InspectionRecommendationReport rebuild() {
        lastReport = analyzer.analyze(assessments.findAll());
        recommendations.replaceAll(lastReport.recommendations());
        return lastReport;
    }

    public InspectionRecommendationReport lastReport() { return lastReport; }
    public Optional<InspectionRecommendation> findById(InspectionRecommendationId id) { return recommendations.findById(id); }
    public List<InspectionRecommendation> findAll() { return recommendations.findAll(); }
    public long count() { return recommendations.count(); }
}
