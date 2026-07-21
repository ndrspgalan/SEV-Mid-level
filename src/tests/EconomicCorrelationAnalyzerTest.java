package tests;

import banking.census.ProfessionCatalog;
import banking.identity.*;
import behavior.alignment.profile.*;
import behavior.correlation.analysis.*;
import behavior.correlation.profile.*;
import behavior.evidence.casefile.BehaviorEvidenceSetId;
import behavior.temporal.*;
import operationalControl.profile.ValerianProfessionCreditProfiles;
import java.time.*;
import java.util.*;

public final class EconomicCorrelationAnalyzerTest {
    public static void main(String[] args) {
        ProfessionCatalog catalog = ProfessionCatalog.valerianStandard();
        Profession maestro = catalog.require("Maestro");
        Profession curtidor = catalog.require("Curtidor");
        SeasonPeriod period = new ValerianSeasonResolver(ZoneOffset.UTC)
                .resolve(Instant.parse("2025-11-03T10:00:00Z"));

        StructuralAlignment maestroToCurtidor = alignment(
                maestro, period, Set.of("Maestro", "Curtidor"));
        StructuralAlignment curtidorToMaestro = alignment(
                curtidor, period, Set.of("Curtidor", "Maestro"));

        EconomicCorrelationAnalysisReport report = new EconomicCorrelationAnalyzer().analyze(
                List.of(maestroToCurtidor, curtidorToMaestro),
                ValerianProfessionCreditProfiles.all());

        check(report.alignmentsExamined() == 2, "alignments");
        check(report.seasonsExamined() == 1, "season");
        check(report.pairComparisons() == 1, "pair comparison");
        check(report.correlationsProduced() == 1, "correlation");
        check(report.mirrorCorrelations() == 1, "mirror");

        EconomicCorrelationGraph graph = report.graphs().get(0);
        check(graph.alignments().size() == 2, "nodes");
        check(graph.clusters().size() == 1, "connected cluster");
        check(graph.displacements().stream().anyMatch(d ->
                d.declaredProfession().equals("Maestro")
                        && d.compatibleProfession().equals("Curtidor")
                        && d.direction() == AlignmentDisplacementDirection.DOWNWARD),
                "downward displacement");
        check(graph.displacements().stream().anyMatch(d ->
                d.declaredProfession().equals("Curtidor")
                        && d.compatibleProfession().equals("Maestro")
                        && d.direction() == AlignmentDisplacementDirection.UPWARD),
                "upward displacement");

        EconomicCorrelation correlation = graph.correlations().get(0);
        check(correlation.types().contains(EconomicCorrelationType.RECIPROCAL_PROFILE_COMPATIBILITY),
                "reciprocal compatibility");
        check(correlation.types().contains(EconomicCorrelationType.MIRROR_DISPLACEMENT),
                "mirror type");
        check(correlation.mirrorBridges().contains("Curtidor<->Maestro"), "bridge");

        InstitutionalProfileTopology topology = new InstitutionalProfileTopology(
                ValerianProfessionCreditProfiles.all().stream().map(CreditProfileDescriptor::from).toList());
        check(topology.relation("Maestro", "Curtidor") == InstitutionalProfileRelation.MORE_RESTRICTIVE,
                "institutional downward relation");
        check(topology.relation("Curtidor", "Maestro") == InstitutionalProfileRelation.MORE_EXPANSIVE,
                "institutional upward relation");

        System.out.println("EconomicCorrelationAnalyzerTest: PASSED");
    }

    private static StructuralAlignment alignment(
            Profession profession,
            SeasonPeriod period,
            Set<String> compatibleProfiles
    ) {
        ConsumerId consumerId = ConsumerId.random();
        BehaviorEvidenceSetId caseId = BehaviorEvidenceSetId.of(consumerId, profession.code(), period);
        Map<String, ProfileCompatibility> comparisons = new LinkedHashMap<>();
        for (var profile : ValerianProfessionCreditProfiles.all()) {
            CreditProfileDescriptor descriptor = CreditProfileDescriptor.from(profile);
            EnumMap<CreditProfileDimension, DimensionCompatibility> dimensions =
                    new EnumMap<>(CreditProfileDimension.class);
            dimensions.put(CreditProfileDimension.SEASON_INTERACTION_CAPACITY,
                    compatibleProfiles.contains(profile.profession())
                            ? DimensionCompatibility.COMPATIBLE
                            : DimensionCompatibility.INCOMPATIBLE);
            comparisons.put(profile.profession(), new ProfileCompatibility(
                    descriptor, dimensions, List.of("test fixture")));
        }
        return new StructuralAlignment(
                StructuralAlignmentId.from(caseId), caseId, consumerId, profession, period, comparisons);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
