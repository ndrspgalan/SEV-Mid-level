package tests;

import application.ValerianEconomicSystem;
import application.ValerianEconomicSystemBootstrap;
import application.inspection.SeasonalDoctrineUnificationService;
import behavior.health.profile.EconomicHealthAssessmentId;
import behavior.recommendation.profile.*;
import behavior.temporal.*;
import inspection.casefile.*;
import inspection.repository.*;
import inspection.doctrine.casefile.*;
import inspection.doctrine.repository.*;
import inspection.jurisprudence.casefile.*;
import inspection.jurisprudence.repository.*;
import inspection.resolution.casefile.*;
import inspection.resolution.repository.*;
import java.time.*;
import java.util.*;

public final class SeasonalDoctrineUnificationServiceTest {
    public static void main(String[] args) {
        SeasonPeriod spring = new SeasonPeriod(Season.SPRING, 2026,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31));
        Instant instant = Instant.parse("2026-05-31T23:59:00Z");
        InMemoryInspectionCaseRepository cases = new InMemoryInspectionCaseRepository();
        InMemoryInspectionResolutionRepository resolutions = new InMemoryInspectionResolutionRepository();
        InMemoryJurisprudentialComparisonRepository comparisons = new InMemoryJurisprudentialComparisonRepository();
        InMemoryUnifiedDoctrineCaseRepository doctrines = new InMemoryUnifiedDoctrineCaseRepository();
        SeasonalDoctrineUnificationService service = new SeasonalDoctrineUnificationService(
                cases, resolutions, comparisons, doctrines, Clock.fixed(instant, ZoneOffset.UTC));

        JurisprudentialSimilarityKey absoluteKey = family(cases, resolutions, comparisons, spring, instant,
                "absolute", InspectionRecommendationReason.MIRROR_MOVEMENT,
                List.of(InspectionResolutionType.TO_FILE, InspectionResolutionType.TO_FILE, InspectionResolutionType.TO_FILE),
                List.of(JurisprudentialAgreement.NO_CONSTA, JurisprudentialAgreement.YES, JurisprudentialAgreement.YES));
        JurisprudentialSimilarityKey relativeKey = family(cases, resolutions, comparisons, spring, instant,
                "relative", InspectionRecommendationReason.PROFESSIONAL_CONVERGENCE,
                List.of(InspectionResolutionType.AUDIT, InspectionResolutionType.AUDIT,
                        InspectionResolutionType.AUDIT, InspectionResolutionType.FRAUD),
                List.of(JurisprudentialAgreement.NO_CONSTA, JurisprudentialAgreement.YES,
                        JurisprudentialAgreement.YES, JurisprudentialAgreement.NO));
        JurisprudentialSimilarityKey nullKey = family(cases, resolutions, comparisons, spring, instant,
                "null", InspectionRecommendationReason.PROFESSIONAL_DIVERGENCE,
                List.of(InspectionResolutionType.AUDIT, InspectionResolutionType.FRAUD),
                List.of(JurisprudentialAgreement.NO_CONSTA, JurisprudentialAgreement.NO_CONSTA));
        JurisprudentialSimilarityKey disagreementKey = family(cases, resolutions, comparisons, spring, instant,
                "disagreement", InspectionRecommendationReason.SYSTEMIC_ALIGNMENT,
                List.of(InspectionResolutionType.FRAUD, InspectionResolutionType.FRAUD),
                List.of(JurisprudentialAgreement.NO, JurisprudentialAgreement.NO));
        // Ineligible singleton.
        family(cases, resolutions, comparisons, spring, instant, "single",
                InspectionRecommendationReason.CREDIT_PROFILE_MIGRATION,
                List.of(InspectionResolutionType.TO_FILE), List.of(JurisprudentialAgreement.NO_CONSTA));

        List<UnifiedDoctrineCase> unified = service.unifySeason(spring);
        require(unified.size() == 4, "only families with at least two cases must create doctrine");
        assertDoctrine(service, spring, absoluteKey, InspectionResolutionType.TO_FILE,
                JurisprudentialConsensusType.ABSOLUTE_AGREEMENT, DoctrineUnificationMethod.DOMINANT_AGREEMENT, 3);
        assertDoctrine(service, spring, relativeKey, InspectionResolutionType.AUDIT,
                JurisprudentialConsensusType.RELATIVE_AGREEMENT, DoctrineUnificationMethod.DOMINANT_AGREEMENT, 4);
        assertDoctrine(service, spring, nullKey, InspectionResolutionType.TO_FILE,
                JurisprudentialConsensusType.NULL_AGREEMENT, DoctrineUnificationMethod.FORCED_MODE, 2);
        assertDoctrine(service, spring, disagreementKey, InspectionResolutionType.FRAUD,
                JurisprudentialConsensusType.ABSOLUTE_DISAGREEMENT, DoctrineUnificationMethod.FORCED_MODE, 2);

        // Idempotent replay returns the same immutable doctrine objects.
        require(service.unifySeason(spring).equals(unified), "seasonal unification must be idempotent");
        require(doctrines.count() == 4, "idempotent replay must not duplicate doctrine");

        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();
        require(system.getUnifiedDoctrineCaseRepository() != null, "doctrine repository not wired");
        require(system.getSeasonalDoctrineUnificationService() != null, "doctrine service not wired");
        System.out.println("SeasonalDoctrineUnificationServiceTest: PASSED");
    }

    private static JurisprudentialSimilarityKey family(
            InMemoryInspectionCaseRepository cases,
            InMemoryInspectionResolutionRepository resolutions,
            InMemoryJurisprudentialComparisonRepository comparisons,
            SeasonPeriod season, Instant instant, String label,
            InspectionRecommendationReason reason,
            List<InspectionResolutionType> resolutionTypes,
            List<JurisprudentialAgreement> agreements
    ) {
        if (resolutionTypes.size() != agreements.size()) throw new IllegalArgumentException();
        JurisprudentialSimilarityKey key = null;
        for (int i = 0; i < resolutionTypes.size(); i++) {
            EconomicHealthAssessmentId assessmentId = new EconomicHealthAssessmentId("health:m5.2:" + label + ":" + i);
            InspectionRecommendation recommendation = new InspectionRecommendation(
                    InspectionRecommendationId.from(assessmentId), assessmentId, season,
                    reason == InspectionRecommendationReason.SYSTEMIC_ALIGNMENT
                            ? InspectionRecommendationType.SYSTEMIC : InspectionRecommendationType.GROUP,
                    Set.of(reason), new InspectionRecommendationExplanation(
                    "M5.2 fixture", Set.of(), Set.of(), Set.of(), Set.of("Profession-" + label), Set.of("Profile-" + label)));
            InspectionCase inspectionCase = InspectionCase.open(recommendation, instant).close();
            cases.save(inspectionCase);
            key = JurisprudentialSimilarityKey.from(recommendation);
            InspectionResolutionType type = resolutionTypes.get(i);
            resolutions.save(new InspectionResolution(
                    InspectionResolutionId.from(inspectionCase.id()), inspectionCase.id(), season, type,
                    plan(type), new InspectionResolutionExplanation("M5.2 fixture resolution", Set.of(), Set.of()), instant));
            Optional<InspectionResolutionType> dominant = agreements.get(i) == JurisprudentialAgreement.NO_CONSTA
                    ? Optional.empty()
                    : Optional.of(agreements.get(i) == JurisprudentialAgreement.YES ? type : different(type));
            comparisons.save(new JurisprudentialComparison(
                    JurisprudentialComparisonId.from(inspectionCase.id()), inspectionCase.id(), season, key,
                    Set.of(), type, dominant, agreements.get(i), instant));
        }
        return Objects.requireNonNull(key);
    }

    private static InstitutionalActionPlan plan(InspectionResolutionType type) {
        PolicyAdjustment adjustment = new PolicyAdjustment(InstitutionalActionScope.SYSTEM,
                PolicyAdjustmentDirection.RESTRICT, "TEST_POLICY", "M5.2 test adjustment");
        RestrictiveMeasure restriction = new RestrictiveMeasure(InstitutionalActionScope.GROUP,
                "TEST_GROUP", "M5.2 test restriction");
        return switch (type) {
            case TO_FILE -> InstitutionalActionPlan.empty();
            case AUDIT -> new InstitutionalActionPlan(List.of(adjustment), List.of());
            case FRAUD -> new InstitutionalActionPlan(List.of(adjustment), List.of(restriction));
        };
    }

    private static InspectionResolutionType different(InspectionResolutionType type) {
        return type == InspectionResolutionType.TO_FILE ? InspectionResolutionType.AUDIT : InspectionResolutionType.TO_FILE;
    }

    private static void assertDoctrine(SeasonalDoctrineUnificationService service, SeasonPeriod season,
            JurisprudentialSimilarityKey key, InspectionResolutionType resolution,
            JurisprudentialConsensusType consensus, DoctrineUnificationMethod method, int value) {
        UnifiedDoctrineCase doctrine = service.findBySeasonAndKey(season, key).orElseThrow();
        require(doctrine.unifiedResolution() == resolution, "unexpected unified resolution");
        require(doctrine.consensusType() == consensus, "unexpected consensus type");
        require(doctrine.unificationMethod() == method, "unexpected unification method");
        require(doctrine.doctrinalValue() == value, "unexpected doctrinal value");
        require(doctrine.sourceInspectionCases().size() == value, "source trace size mismatch");
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
