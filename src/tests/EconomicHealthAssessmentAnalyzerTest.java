package tests;

import behavior.alignment.profile.*;
import behavior.correlation.analysis.EconomicCorrelationAnalyzer;
import behavior.correlation.profile.*;
import behavior.health.analysis.*;
import behavior.health.profile.*;
import behavior.temporal.*;
import banking.identity.*;
import banking.census.ProfessionCatalog;
import behavior.evidence.casefile.BehaviorEvidenceSetId;
import operationalControl.profile.*;
import java.time.*;
import java.util.*;

public final class EconomicHealthAssessmentAnalyzerTest {
    public static void main(String[] args) {
        SeasonPeriod season = new SeasonPeriod(Season.SPRING, 2026, LocalDate.of(2026,3,1), LocalDate.of(2026,5,31));
        ProfessionCatalog catalog = ProfessionCatalog.valerianStandard();
        StructuralAlignment stable = alignment(catalog.require("Maestro"), season, Set.of("Maestro"));
        EconomicCorrelationGraph stableGraph = new EconomicCorrelationAnalyzer()
                .analyze(List.of(stable), ValerianProfessionCreditProfiles.all()).graphs().get(0);
        EconomicHealthAssessment stableAssessment = new EconomicHealthAssessmentAnalyzer().analyze(List.of(stableGraph)).assessments().get(0);
        require(stableAssessment.status() == EconomicHealthStatus.UNKNOWN, "an isolated native alignment cannot establish system health");
        require(stableAssessment.observations().stream().anyMatch(o -> o.type() == EconomicHealthObservationType.STABLE_ALIGNMENT), "stable observation missing");

        StructuralAlignment migrated1 = alignment(catalog.require("Maestro"), season, Set.of("Curtidor"));
        StructuralAlignment migrated2 = alignment(catalog.require("Maestro"), season, Set.of("Curtidor"));
        EconomicCorrelationGraph migratedGraph = new EconomicCorrelationAnalyzer()
                .analyze(List.of(migrated1, migrated2), ValerianProfessionCreditProfiles.all()).graphs().get(0);
        EconomicHealthAssessment migratedAssessment = new EconomicHealthAssessmentAnalyzer().analyze(List.of(migratedGraph)).assessments().get(0);
        require(migratedAssessment.status() == EconomicHealthStatus.CONCERNING, "complete non-native professional migration must be concerning");
        require(migratedAssessment.observations().stream().anyMatch(o -> o.type() == EconomicHealthObservationType.CREDIT_PROFILE_MIGRATION && o.scope() == ObservationScope.PROFESSION), "migration observation missing");
        System.out.println("EconomicHealthAssessmentAnalyzerTest: PASSED");
    }

    private static StructuralAlignment alignment(Profession profession, SeasonPeriod season, Set<String> compatible) {
        ConsumerId consumerId = ConsumerId.random();
        BehaviorEvidenceSetId source = BehaviorEvidenceSetId.of(consumerId, profession.code(), season);
        Map<String, ProfileCompatibility> comparisons = new LinkedHashMap<>();
        for (ProfessionCreditProfile p : ValerianProfessionCreditProfiles.all()) {
            EnumMap<CreditProfileDimension, DimensionCompatibility> dimensions = new EnumMap<>(CreditProfileDimension.class);
            dimensions.put(CreditProfileDimension.SEASON_INTERACTION_CAPACITY, compatible.contains(p.profession()) ? DimensionCompatibility.COMPATIBLE : DimensionCompatibility.INCOMPATIBLE);
            comparisons.put(p.profession(), new ProfileCompatibility(CreditProfileDescriptor.from(p), dimensions, List.of("test fixture")));
        }
        return new StructuralAlignment(StructuralAlignmentId.from(source), source, consumerId, profession, season, comparisons);
    }

    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
