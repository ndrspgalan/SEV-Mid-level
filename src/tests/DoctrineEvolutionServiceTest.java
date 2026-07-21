package tests;

import application.ValerianEconomicSystem;
import application.ValerianEconomicSystemBootstrap;
import application.inspection.DoctrineEvolutionService;
import behavior.recommendation.profile.*;
import behavior.temporal.*;
import inspection.casefile.InspectionCaseId;
import inspection.doctrine.casefile.*;
import inspection.doctrine.repository.*;
import inspection.doctrine.refund.casefile.*;
import inspection.doctrine.refund.repository.*;
import inspection.jurisprudence.casefile.JurisprudentialSimilarityKey;
import inspection.resolution.casefile.InspectionResolutionType;
import java.time.*;
import java.util.*;

public final class DoctrineEvolutionServiceTest {
    public static void main(String[] args) {
        InMemoryUnifiedDoctrineCaseRepository seasonal = new InMemoryUnifiedDoctrineCaseRepository();
        InMemoryRefundedDoctrineCaseRepository refunded = new InMemoryRefundedDoctrineCaseRepository();
        Clock clock = Clock.fixed(Instant.parse("2026-12-31T00:00:00Z"), ZoneOffset.UTC);
        DoctrineEvolutionService service = new DoctrineEvolutionService(seasonal, refunded, clock);

        JurisprudentialSimilarityKey continuity = key(InspectionRecommendationReason.CREDIT_PROFILE_MIGRATION);
        JurisprudentialSimilarityKey weightedConflict = key(InspectionRecommendationReason.MIRROR_MOVEMENT);
        JurisprudentialSimilarityKey disappearance = key(InspectionRecommendationReason.SYSTEMIC_ALIGNMENT);

        SeasonPeriod spring = season(Season.SPRING, 2026, 4, 1, 6, 30);
        SeasonPeriod summer = season(Season.SUMMER, 2026, 7, 1, 9, 30);
        SeasonPeriod autumn = season(Season.AUTUMN, 2026, 10, 1, 12, 31);

        seasonal.save(doctrine(spring, continuity, InspectionResolutionType.AUDIT, 8, 1));
        seasonal.save(doctrine(summer, continuity, InspectionResolutionType.AUDIT, 5, 2));
        RefundedDoctrineCase continuous = service.refund(continuity).orElseThrow();
        require(continuous.currentOutcome() == RefundedDoctrineOutcome.AUDIT, "continuity must retain AUDIT");
        require(continuous.currentResolution().orElseThrow() == InspectionResolutionType.AUDIT, "resolution missing");
        require(continuous.doctrinalValue() == 13, "values must accumulate");
        require(continuous.coveredSeasons().size() == 2, "covered seasons mismatch");

        seasonal.save(doctrine(spring, weightedConflict, InspectionResolutionType.AUDIT, 8, 3));
        seasonal.save(doctrine(summer, weightedConflict, InspectionResolutionType.FRAUD, 3, 4));
        RefundedDoctrineCase weighted = service.refund(weightedConflict).orElseThrow();
        require(weighted.currentOutcome() == RefundedDoctrineOutcome.AUDIT, "higher doctrinal weight must win");
        require(weighted.doctrinalValue() == 11, "weighted total mismatch");

        seasonal.save(doctrine(autumn, weightedConflict, InspectionResolutionType.FRAUD, 5, 5));
        RefundedDoctrineCase conflict = service.refund(weightedConflict).orElseThrow();
        require(conflict.currentOutcome() == RefundedDoctrineOutcome.DOCTRINAL_CONFLICT, "equal weights must conflict");
        require(conflict.currentResolution().isEmpty(), "conflict must not expose a resolution");
        require(conflict.doctrinalValue() == 16, "conflict must retain all doctrinal value");

        seasonal.save(doctrine(spring, disappearance, InspectionResolutionType.TO_FILE, 4, 6));
        RefundedDoctrineCase persistent = service.refund(disappearance).orElseThrow();
        require(persistent.currentOutcome() == RefundedDoctrineOutcome.TO_FILE, "a family remains alive when absent later");
        require(persistent.coveredSeasons().equals(Set.of(spring)), "absence must not invent seasons");

        List<RefundedDoctrineCase> first = service.refundAll();
        List<RefundedDoctrineCase> second = service.refundAll();
        require(first.size() == 3 && second.size() == 3 && service.count() == 3, "refund must be idempotent");

        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();
        require(system.getRefundedDoctrineCaseRepository() != null, "refunded repository not wired");
        require(system.getDoctrineEvolutionService() != null, "evolution service not wired");
        System.out.println("DoctrineEvolutionServiceTest: PASSED");
    }

    private static JurisprudentialSimilarityKey key(InspectionRecommendationReason reason) {
        return new JurisprudentialSimilarityKey(
                InspectionRecommendationType.PROFESSION,
                Set.of(reason), Set.of("MERCHANT"), Set.of("CREDIT"));
    }

    private static SeasonPeriod season(Season season, int year, int sm, int sd, int em, int ed) {
        return new SeasonPeriod(season, year, LocalDate.of(year, sm, sd), LocalDate.of(year, em, ed));
    }

    private static UnifiedDoctrineCase doctrine(
            SeasonPeriod season,
            JurisprudentialSimilarityKey key,
            InspectionResolutionType resolution,
            int value,
            int seed
    ) {
        LinkedHashSet<InspectionCaseId> sources = new LinkedHashSet<>();
        for (int i = 0; i < value; i++) sources.add(new InspectionCaseId("CASE-" + seed + "-" + i));
        return new UnifiedDoctrineCase(
                UnifiedDoctrineCaseId.from(season, key), season, key, resolution,
                JurisprudentialConsensusType.ABSOLUTE_AGREEMENT,
                DoctrineUnificationMethod.DOMINANT_AGREEMENT,
                value, sources, Instant.parse("2026-01-01T00:00:00Z"));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
