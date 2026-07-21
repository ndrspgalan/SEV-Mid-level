package tests;

import application.ValerianEconomicSystem;
import application.ValerianEconomicSystemBootstrap;
import application.inspection.InspectionCaseService;
import behavior.health.profile.EconomicHealthAssessmentId;
import behavior.recommendation.profile.*;
import behavior.recommendation.repository.InMemoryInspectionRecommendationRepository;
import behavior.temporal.*;
import inspection.casefile.*;
import inspection.repository.InMemoryInspectionCaseRepository;
import java.time.*;
import java.util.*;

public final class InspectionCaseServiceTest {
    public static void main(String[] args) {
        SeasonPeriod season = new SeasonPeriod(
                Season.SPRING,
                2026,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 5, 31));
        EconomicHealthAssessmentId assessmentId = new EconomicHealthAssessmentId("health:test:m4.1");
        InspectionRecommendationId recommendationId = InspectionRecommendationId.from(assessmentId);

        InspectionRecommendation actionable = new InspectionRecommendation(
                recommendationId,
                assessmentId,
                season,
                InspectionRecommendationType.PROFESSION,
                Set.of(InspectionRecommendationReason.CREDIT_PROFILE_MIGRATION),
                new InspectionRecommendationExplanation(
                        "Professional inspection is institutionally justified.",
                        Set.of(), Set.of(), Set.of(), Set.of("Maestro"), Set.of("Curtidor")));

        InMemoryInspectionRecommendationRepository recommendations =
                new InMemoryInspectionRecommendationRepository();
        recommendations.replaceAll(List.of(actionable));
        InMemoryInspectionCaseRepository cases = new InMemoryInspectionCaseRepository();
        Instant openedAt = Instant.parse("2026-04-10T10:15:30Z");
        InspectionCaseService service = new InspectionCaseService(
                recommendations,
                cases,
                Clock.fixed(openedAt, ZoneOffset.UTC));

        InspectionCase inspectionCase = service.open(recommendationId);
        require(inspectionCase.id().equals(InspectionCaseId.from(recommendationId)), "case identity mismatch");
        require(inspectionCase.status() == InspectionCaseStatus.OPEN, "new case must be open");
        require(inspectionCase.openedAt().equals(openedAt), "opening instant mismatch");
        require(inspectionCase.openedSeason().equals(season), "opening season must come from recommendation");
        require(inspectionCase.sourceRecommendation().equals(actionable), "recommendation snapshot must be retained");
        require(inspectionCase.sourceRecommendation().type() == InspectionRecommendationType.PROFESSION,
                "institutional origin must remain the recommendation scope");

        InspectionCase sameCase = service.open(recommendationId);
        require(sameCase == inspectionCase, "opening the same recommendation must be idempotent");
        require(service.count() == 1, "idempotent opening must not duplicate cases");

        InspectionRecommendation none = new InspectionRecommendation(
                InspectionRecommendationId.from(new EconomicHealthAssessmentId("health:test:none")),
                new EconomicHealthAssessmentId("health:test:none"),
                season,
                InspectionRecommendationType.NONE,
                Set.of(InspectionRecommendationReason.INSUFFICIENT_EVIDENCE),
                new InspectionRecommendationExplanation(
                        "No inspection is justified.",
                        Set.of(), Set.of(), Set.of(), Set.of(), Set.of()));
        boolean rejected = false;
        try {
            service.open(none);
        } catch (IllegalArgumentException expected) {
            rejected = true;
        }
        require(rejected, "NONE recommendation must not open a case");

        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();
        require(system.getInspectionCaseRepository() != null, "inspection case repository not wired");
        require(system.getInspectionCaseService() != null, "inspection case service not wired");

        System.out.println("InspectionCaseServiceTest: PASSED");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
