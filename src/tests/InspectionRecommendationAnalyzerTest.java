package tests;

import application.ValerianEconomicSystem;
import application.ValerianEconomicSystemBootstrap;
import behavior.alignment.profile.StructuralAlignmentId;
import behavior.correlation.profile.EconomicCorrelationGraphId;
import behavior.health.analysis.EconomicHealthAssessmentAnalyzer;
import behavior.health.profile.*;
import behavior.recommendation.analysis.*;
import behavior.recommendation.profile.*;
import behavior.temporal.*;
import java.time.LocalDate;
import java.util.*;

public final class InspectionRecommendationAnalyzerTest {
    public static void main(String[] args) {
        SeasonPeriod season = new SeasonPeriod(Season.SPRING, 2026, LocalDate.of(2026,3,1), LocalDate.of(2026,5,31));
        EconomicCorrelationGraphId graphId = new EconomicCorrelationGraphId("correlation:test:spring-2026");
        EconomicHealthAssessmentId assessmentId = EconomicHealthAssessmentId.from(graphId);

        EconomicHealthObservation concerning = new EconomicHealthObservation(
                EconomicHealthObservationId.of(assessmentId, 1),
                EconomicHealthObservationType.CREDIT_PROFILE_MIGRATION,
                ObservationScope.PROFESSION,
                EconomicHealthStatus.CONCERNING,
                new EconomicHealthExplanation(
                        "Migración profesional completa.",
                        Set.of(new StructuralAlignmentId("alignment:test")),
                        Set.of(),
                        Set.of("Maestro"),
                        Set.of("Curtidor")));
        EconomicHealthAssessment migrated = new EconomicHealthAssessment(
                assessmentId, graphId, season, EconomicHealthStatus.CONCERNING, List.of(concerning));

        InspectionRecommendation recommendation = new InspectionRecommendationAnalyzer()
                .analyze(List.of(migrated)).recommendations().get(0);
        require(recommendation.type() == InspectionRecommendationType.PROFESSION,
                "profession observation must produce profession recommendation");
        require(recommendation.recommendsInspection(), "concerning assessment must recommend inspection");
        require(recommendation.reasons().contains(InspectionRecommendationReason.CREDIT_PROFILE_MIGRATION),
                "migration reason missing");
        require(recommendation.explanation().observationEvidence().contains(concerning.id()),
                "observation trace missing");

        EconomicHealthObservation unknown = new EconomicHealthObservation(
                EconomicHealthObservationId.of(assessmentId, 2),
                EconomicHealthObservationType.INSUFFICIENT_EVIDENCE,
                ObservationScope.SYSTEM,
                EconomicHealthStatus.UNKNOWN,
                new EconomicHealthExplanation("Evidencia insuficiente.", Set.of(), Set.of(), Set.of(), Set.of()));
        EconomicHealthAssessment inconclusive = new EconomicHealthAssessment(
                assessmentId, graphId, season, EconomicHealthStatus.UNKNOWN, List.of(unknown));
        InspectionRecommendation none = new InspectionRecommendationAnalyzer()
                .analyze(List.of(inconclusive)).recommendations().get(0);
        require(none.type() == InspectionRecommendationType.NONE, "unknown assessment must not open inspection");
        require(none.reasons().contains(InspectionRecommendationReason.INSUFFICIENT_EVIDENCE),
                "inconclusive reason missing");

        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();
        require(system.getInspectionRecommendationRepository() != null, "recommendation repository not wired");
        require(system.getInspectionRecommendationService() != null, "recommendation service not wired");

        require(InspectionRecommendationType.mostExtensive(
                InspectionRecommendationType.GROUP,
                InspectionRecommendationType.SYSTEMIC) == InspectionRecommendationType.SYSTEMIC,
                "scope order must define institutional urgency");

        System.out.println("InspectionRecommendationAnalyzerTest: PASSED");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
