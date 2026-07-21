package tests;

import application.ValerianEconomicSystem;
import application.ValerianEconomicSystemBootstrap;
import application.inspection.*;
import behavior.health.profile.EconomicHealthAssessmentId;
import behavior.recommendation.profile.*;
import behavior.recommendation.repository.InMemoryInspectionRecommendationRepository;
import behavior.temporal.*;
import inspection.casefile.*;
import inspection.repository.InMemoryInspectionCaseRepository;
import inspection.investigation.repository.*;
import inspection.resolution.casefile.*;
import inspection.resolution.repository.*;
import inspection.jurisprudence.casefile.*;
import inspection.jurisprudence.repository.*;
import java.time.*;
import java.util.*;

public final class JurisprudentialComparisonServiceTest {
    public static void main(String[] args) {
        SeasonPeriod spring2026 = season(Season.SPRING, 2026);
        SeasonPeriod spring2027 = season(Season.SPRING, 2027);
        Clock clock = Clock.fixed(Instant.parse("2026-05-30T12:00:00Z"), ZoneOffset.UTC);

        InMemoryInspectionRecommendationRepository recommendations = new InMemoryInspectionRecommendationRepository();
        InMemoryInspectionCaseRepository cases = new InMemoryInspectionCaseRepository();
        InMemoryEvidenceRecordRepository evidence = new InMemoryEvidenceRecordRepository();
        InMemoryHypothesisRecordRepository hypotheses = new InMemoryHypothesisRecordRepository();
        InMemoryInspectionResolutionRepository resolutions = new InMemoryInspectionResolutionRepository();
        InMemoryJurisprudentialComparisonRepository comparisons = new InMemoryJurisprudentialComparisonRepository();
        InspectionCaseService caseService = new InspectionCaseService(recommendations, cases, clock);
        JurisprudentialComparisonService jurisprudence = new JurisprudentialComparisonService(
                cases, resolutions, comparisons, clock);
        InspectionResolutionService resolutionService = new InspectionResolutionService(
                cases, evidence, hypotheses, resolutions, jurisprudence, clock);

        InspectionRecommendation first = recommendation("health:m5.1:first", spring2026,
                InspectionRecommendationType.GROUP, Set.of(InspectionRecommendationReason.MIRROR_MOVEMENT),
                Set.of("Comerciante", "Noble"), Set.of("Comerciante<->Noble"));
        InspectionRecommendation second = recommendation("health:m5.1:second", spring2026,
                InspectionRecommendationType.GROUP, Set.of(InspectionRecommendationReason.MIRROR_MOVEMENT),
                Set.of("Noble", "Comerciante"), Set.of("Comerciante<->Noble"));
        InspectionRecommendation third = recommendation("health:m5.1:third", spring2026,
                InspectionRecommendationType.GROUP, Set.of(InspectionRecommendationReason.MIRROR_MOVEMENT),
                Set.of("Comerciante", "Noble"), Set.of("Comerciante<->Noble"));
        InspectionRecommendation fourth = recommendation("health:m5.1:fourth", spring2026,
                InspectionRecommendationType.GROUP, Set.of(InspectionRecommendationReason.MIRROR_MOVEMENT),
                Set.of("Comerciante", "Noble"), Set.of("Comerciante<->Noble"));
        InspectionRecommendation differentKey = recommendation("health:m5.1:different", spring2026,
                InspectionRecommendationType.GROUP, Set.of(InspectionRecommendationReason.PROFESSIONAL_CONVERGENCE),
                Set.of("Comerciante", "Noble"), Set.of("Mercenario"));
        InspectionRecommendation differentSeason = recommendation("health:m5.1:next-season", spring2027,
                InspectionRecommendationType.GROUP, Set.of(InspectionRecommendationReason.MIRROR_MOVEMENT),
                Set.of("Comerciante", "Noble"), Set.of("Comerciante<->Noble"));
        recommendations.replaceAll(List.of(first, second, third, fourth, differentKey, differentSeason));

        InspectionCase firstCase = caseService.open(first);
        resolve(resolutionService, firstCase.id(), InspectionResolutionType.TO_FILE);
        JurisprudentialComparison firstComparison = comparisons.findByCaseId(firstCase.id()).orElseThrow();
        require(firstComparison.comparedCases().isEmpty(), "first case must have no precedents");
        require(firstComparison.dominantResolution().isEmpty(), "first case must have no dominant jurisprudence");
        require(firstComparison.agreement() == JurisprudentialAgreement.NO_CONSTA, "first case must be NO_CONSTA");

        InspectionCase secondCase = caseService.open(second);
        resolve(resolutionService, secondCase.id(), InspectionResolutionType.TO_FILE);
        JurisprudentialComparison secondComparison = comparisons.findByCaseId(secondCase.id()).orElseThrow();
        require(secondComparison.comparedCases().equals(Set.of(firstCase.id())), "second case must compare only first case");
        require(secondComparison.dominantResolution().orElseThrow() == InspectionResolutionType.TO_FILE,
                "TO_FILE must be dominant");
        require(secondComparison.agreement() == JurisprudentialAgreement.YES, "equal resolution must be YES");

        InspectionCase thirdCase = caseService.open(third);
        resolve(resolutionService, thirdCase.id(), InspectionResolutionType.AUDIT);
        JurisprudentialComparison thirdComparison = comparisons.findByCaseId(thirdCase.id()).orElseThrow();
        require(thirdComparison.comparedCases().equals(Set.of(firstCase.id(), secondCase.id())),
                "third case must compare both earlier similar cases");
        require(thirdComparison.dominantResolution().orElseThrow() == InspectionResolutionType.TO_FILE,
                "two TO_FILE precedents must dominate");
        require(thirdComparison.agreement() == JurisprudentialAgreement.NO, "different resolution must be NO");

        InspectionCase differentCase = caseService.open(differentKey);
        resolve(resolutionService, differentCase.id(), InspectionResolutionType.FRAUD);
        JurisprudentialComparison differentComparison = comparisons.findByCaseId(differentCase.id()).orElseThrow();
        require(differentComparison.comparedCases().isEmpty(), "different financial identity must not be compared");

        InspectionCase fourthCase = caseService.open(fourth);
        resolve(resolutionService, fourthCase.id(), InspectionResolutionType.FRAUD);
        JurisprudentialComparison fourthComparison = comparisons.findByCaseId(fourthCase.id()).orElseThrow();
        require(fourthComparison.comparedCases().size() == 3, "fourth case must compare three similar precedents");
        require(fourthComparison.dominantResolution().orElseThrow() == InspectionResolutionType.TO_FILE,
                "TO_FILE remains the unique mode at 2-1");
        require(fourthComparison.agreement() == JurisprudentialAgreement.NO, "FRAUD differs from TO_FILE");

        // A later Season has the same financial identity but no same-Season precedent.
        InspectionCase nextSeasonCase = caseService.open(differentSeason);
        resolve(resolutionService, nextSeasonCase.id(), InspectionResolutionType.TO_FILE);
        JurisprudentialComparison nextSeasonComparison = comparisons.findByCaseId(nextSeasonCase.id()).orElseThrow();
        require(nextSeasonComparison.comparedCases().isEmpty(), "same identity from another Season must be excluded in M5.1");
        require(nextSeasonComparison.similarityKey().equals(firstComparison.similarityKey()),
                "similarity identity must remain reusable across Seasons");

        // Explicit tie: one TO_FILE and one AUDIT in a separate family.
        InspectionRecommendation tieA = recommendation("health:m5.1:tie-a", spring2026,
                InspectionRecommendationType.PROFESSION, Set.of(InspectionRecommendationReason.CREDIT_PROFILE_MIGRATION),
                Set.of("Jornalero"), Set.of("Mercader"));
        InspectionRecommendation tieB = recommendation("health:m5.1:tie-b", spring2026,
                InspectionRecommendationType.PROFESSION, Set.of(InspectionRecommendationReason.CREDIT_PROFILE_MIGRATION),
                Set.of("Jornalero"), Set.of("Mercader"));
        InspectionRecommendation tieCurrent = recommendation("health:m5.1:tie-current", spring2026,
                InspectionRecommendationType.PROFESSION, Set.of(InspectionRecommendationReason.CREDIT_PROFILE_MIGRATION),
                Set.of("Jornalero"), Set.of("Mercader"));
        recommendations.replaceAll(new ArrayList<>() {{ addAll(List.of(first, second, third, fourth, differentKey, differentSeason, tieA, tieB, tieCurrent)); }});
        InspectionCase tieCaseA = caseService.open(tieA);
        resolve(resolutionService, tieCaseA.id(), InspectionResolutionType.TO_FILE);
        InspectionCase tieCaseB = caseService.open(tieB);
        resolve(resolutionService, tieCaseB.id(), InspectionResolutionType.AUDIT);
        InspectionCase tieCurrentCase = caseService.open(tieCurrent);
        resolve(resolutionService, tieCurrentCase.id(), InspectionResolutionType.FRAUD);
        JurisprudentialComparison tieComparison = comparisons.findByCaseId(tieCurrentCase.id()).orElseThrow();
        require(tieComparison.comparedCases().size() == 2, "tie comparison must contain both precedents");
        require(tieComparison.dominantResolution().isEmpty(), "tie must have no dominant jurisprudence");
        require(tieComparison.agreement() == JurisprudentialAgreement.NO_CONSTA, "tie must answer NO_CONSTA");

        require(comparisons.count() == resolutions.count(), "every final resolution must have one comparison");
        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();
        require(system.getJurisprudentialComparisonRepository() != null, "comparison repository not wired");
        require(system.getJurisprudentialComparisonService() != null, "comparison service not wired");

        System.out.println("JurisprudentialComparisonServiceTest: PASSED");
    }

    private static void resolve(InspectionResolutionService service, InspectionCaseId id, InspectionResolutionType type) {
        InstitutionalActionPlan plan = switch (type) {
            case TO_FILE -> InstitutionalActionPlan.empty();
            case AUDIT -> new InstitutionalActionPlan(
                    List.of(new PolicyAdjustment(InstitutionalActionScope.SYSTEM,
                            PolicyAdjustmentDirection.RECONFIGURE, "TEST_POLICY", "Reconfigure tested institution.")),
                    List.of());
            case FRAUD -> new InstitutionalActionPlan(
                    List.of(new PolicyAdjustment(InstitutionalActionScope.SYSTEM,
                            PolicyAdjustmentDirection.RESTRICT, "TEST_POLICY", "Restrict tested institution.")),
                    List.of(new RestrictiveMeasure(InstitutionalActionScope.GROUP,
                            "TEST_GROUP", "Restrict responsible group.")));
        };
        service.resolve(id, type, plan,
                new InspectionResolutionExplanation("Test resolution for jurisprudential comparison.", Set.of(), Set.of()));
    }

    private static InspectionRecommendation recommendation(
            String assessmentValue,
            SeasonPeriod season,
            InspectionRecommendationType type,
            Set<InspectionRecommendationReason> reasons,
            Set<String> professions,
            Set<String> profiles
    ) {
        EconomicHealthAssessmentId assessmentId = new EconomicHealthAssessmentId(assessmentValue);
        return new InspectionRecommendation(
                InspectionRecommendationId.from(assessmentId), assessmentId, season, type, reasons,
                new InspectionRecommendationExplanation(
                        "Jurisprudential fixture.", Set.of(), Set.of(), Set.of(), professions, profiles));
    }

    private static SeasonPeriod season(Season season, int year) {
        return switch (season) {
            case SPRING -> new SeasonPeriod(season, year, LocalDate.of(year, 3, 1), LocalDate.of(year, 5, 31));
            case SUMMER -> new SeasonPeriod(season, year, LocalDate.of(year, 6, 1), LocalDate.of(year, 8, 31));
            case AUTUMN -> new SeasonPeriod(season, year, LocalDate.of(year, 9, 1), LocalDate.of(year, 11, 30));
            case WINTER -> new SeasonPeriod(season, year, LocalDate.of(year - 1, 12, 1), LocalDate.of(year, 2, 28));
        };
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
