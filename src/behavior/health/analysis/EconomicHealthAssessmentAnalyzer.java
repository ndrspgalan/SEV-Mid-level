package behavior.health.analysis;

import behavior.alignment.profile.*;
import behavior.correlation.profile.*;
import behavior.health.profile.*;
import java.util.*;
import java.util.stream.Collectors;

/** Interprets M3.5 graphs through exact structural predicates, never scores or configurable thresholds. */
public final class EconomicHealthAssessmentAnalyzer {

    public EconomicHealthAssessmentReport analyze(Collection<EconomicCorrelationGraph> graphs) {
        Objects.requireNonNull(graphs);
        List<EconomicHealthAssessment> assessments = graphs.stream()
                .sorted(Comparator.comparing(g -> g.seasonPeriod().startsOn()))
                .map(this::assess)
                .toList();
        long observations = assessments.stream().mapToLong(a -> a.observations().size()).sum();
        long stable = assessments.stream().filter(a -> a.status() == EconomicHealthStatus.STABLE).count();
        long concerning = assessments.stream().filter(a -> a.status() == EconomicHealthStatus.CONCERNING).count();
        long unknown = assessments.stream().filter(a -> a.status() == EconomicHealthStatus.UNKNOWN).count();
        return new EconomicHealthAssessmentReport(graphs.size(), assessments.size(), observations, stable, concerning, unknown, assessments);
    }

    private EconomicHealthAssessment assess(EconomicCorrelationGraph graph) {
        EconomicHealthAssessmentId id = EconomicHealthAssessmentId.from(graph.id());
        List<ObservationDraft> drafts = new ArrayList<>();

        if (graph.alignments().isEmpty()) {
            drafts.add(new ObservationDraft(EconomicHealthObservationType.INSUFFICIENT_EVIDENCE, ObservationScope.SYSTEM,
                    EconomicHealthStatus.UNKNOWN,
                    new EconomicHealthExplanation("La estación no contiene alineamientos estructurales evaluables.", Set.of(), Set.of(), Set.of(), Set.of())));
        } else {
            addStableAndIsolated(graph, drafts);
            addProfessionalMigrations(graph, drafts);
            addConvergence(graph, drafts);
            addDivergence(graph, drafts);
            addMirrorMovement(graph, drafts);
            addSystemicAlignment(graph, drafts);
        }

        if (drafts.isEmpty()) {
            drafts.add(new ObservationDraft(EconomicHealthObservationType.INSUFFICIENT_EVIDENCE, ObservationScope.SYSTEM,
                    EconomicHealthStatus.UNKNOWN,
                    new EconomicHealthExplanation("El grafo contiene datos, pero no permite formular una interpretación estructural unívoca.", graph.alignments().keySet(), Set.of(), professions(graph.alignments().values()), Set.of())));
        }

        List<EconomicHealthObservation> observations = new ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            ObservationDraft d = drafts.get(i);
            observations.add(new EconomicHealthObservation(EconomicHealthObservationId.of(id, i + 1), d.type, d.scope, d.indicator, d.explanation));
        }
        EconomicHealthStatus status = aggregate(observations);
        return new EconomicHealthAssessment(id, graph.id(), graph.seasonPeriod(), status, observations);
    }

    private static void addStableAndIsolated(EconomicCorrelationGraph graph, List<ObservationDraft> drafts) {
        Map<StructuralAlignmentId, List<AlignmentDisplacement>> byAlignment = graph.displacements().stream()
                .collect(Collectors.groupingBy(AlignmentDisplacement::alignmentId, LinkedHashMap::new, Collectors.toList()));
        Set<StructuralAlignmentId> correlated = new HashSet<>();
        graph.correlations().forEach(c -> { correlated.add(c.firstAlignmentId()); correlated.add(c.secondAlignmentId()); });

        for (StructuralAlignment alignment : graph.alignments().values()) {
            List<AlignmentDisplacement> ds = byAlignment.getOrDefault(alignment.id(), List.of());
            boolean nativeCompatible = ds.stream().anyMatch(d -> d.direction() == AlignmentDisplacementDirection.NATIVE);
            boolean directional = ds.stream().anyMatch(d -> d.direction() == AlignmentDisplacementDirection.UPWARD || d.direction() == AlignmentDisplacementDirection.DOWNWARD);
            if (nativeCompatible && !directional) {
                drafts.add(new ObservationDraft(EconomicHealthObservationType.STABLE_ALIGNMENT, ObservationScope.INDIVIDUAL,
                        EconomicHealthStatus.STABLE,
                        new EconomicHealthExplanation("El sujeto conserva compatibilidad con su perfil declarado y no presenta desplazamiento institucional direccional.", Set.of(alignment.id()), Set.of(), Set.of(alignment.declaredProfession().name()), targets(ds))));
            }
            if (!correlated.contains(alignment.id())) {
                drafts.add(new ObservationDraft(EconomicHealthObservationType.ISOLATED_ALIGNMENT, ObservationScope.INDIVIDUAL,
                        EconomicHealthStatus.UNKNOWN,
                        new EconomicHealthExplanation("El alineamiento no mantiene relaciones descriptivas con otros alineamientos de la estación.", Set.of(alignment.id()), Set.of(), Set.of(alignment.declaredProfession().name()), targets(ds))));
            }
        }
    }

    private static void addProfessionalMigrations(EconomicCorrelationGraph graph, List<ObservationDraft> drafts) {
        Map<String, List<StructuralAlignment>> byProfession = graph.alignments().values().stream()
                .collect(Collectors.groupingBy(a -> a.declaredProfession().name(), TreeMap::new, Collectors.toList()));
        Map<StructuralAlignmentId, List<AlignmentDisplacement>> ds = displacementIndex(graph);
        for (Map.Entry<String, List<StructuralAlignment>> entry : byProfession.entrySet()) {
            Set<String> commonDirectionalTargets = null;
            boolean everyLacksNative = true;
            for (StructuralAlignment a : entry.getValue()) {
                List<AlignmentDisplacement> values = ds.getOrDefault(a.id(), List.of());
                if (values.stream().anyMatch(d -> d.direction() == AlignmentDisplacementDirection.NATIVE)) everyLacksNative = false;
                Set<String> directionalTargets = values.stream()
                        .filter(d -> d.direction() == AlignmentDisplacementDirection.UPWARD || d.direction() == AlignmentDisplacementDirection.DOWNWARD)
                        .map(AlignmentDisplacement::compatibleProfession).collect(Collectors.toCollection(TreeSet::new));
                commonDirectionalTargets = commonDirectionalTargets == null ? directionalTargets : intersection(commonDirectionalTargets, directionalTargets);
            }
            if (everyLacksNative && commonDirectionalTargets != null && !commonDirectionalTargets.isEmpty()) {
                Set<StructuralAlignmentId> ids = entry.getValue().stream().map(StructuralAlignment::id).collect(Collectors.toCollection(LinkedHashSet::new));
                drafts.add(new ObservationDraft(EconomicHealthObservationType.CREDIT_PROFILE_MIGRATION, ObservationScope.PROFESSION,
                        EconomicHealthStatus.CONCERNING,
                        new EconomicHealthExplanation("Todos los sujetos observados de la profesión carecen de compatibilidad nativa y comparten al menos un destino crediticio direccional.", ids, Set.of(), Set.of(entry.getKey()), commonDirectionalTargets)));
            }
        }
    }

    private static void addConvergence(EconomicCorrelationGraph graph, List<ObservationDraft> drafts) {
        Map<String, Set<String>> targetToProfessions = new TreeMap<>();
        Map<String, Set<StructuralAlignmentId>> targetToAlignments = new TreeMap<>();
        for (AlignmentDisplacement d : graph.displacements()) {
            if (d.direction() == AlignmentDisplacementDirection.NATIVE || d.direction() == AlignmentDisplacementDirection.LATERAL) continue;
            targetToProfessions.computeIfAbsent(d.compatibleProfession(), ignored -> new TreeSet<>()).add(d.declaredProfession());
            targetToAlignments.computeIfAbsent(d.compatibleProfession(), ignored -> new LinkedHashSet<>()).add(d.alignmentId());
        }
        for (String target : targetToProfessions.keySet()) {
            Set<String> ps = targetToProfessions.get(target);
            if (ps.size() > 1) {
                drafts.add(new ObservationDraft(EconomicHealthObservationType.PROFESSIONAL_CONVERGENCE, ObservationScope.GROUP,
                        EconomicHealthStatus.CONCERNING,
                        new EconomicHealthExplanation("Profesiones distintas comparten un mismo destino institucional no nativo.", targetToAlignments.get(target), correlationsTouching(graph, targetToAlignments.get(target)), ps, Set.of(target))));
            }
        }
    }

    private static void addDivergence(EconomicCorrelationGraph graph, List<ObservationDraft> drafts) {
        Map<String, Map<StructuralAlignmentId, Set<String>>> byProfession = new TreeMap<>();
        for (AlignmentDisplacement d : graph.displacements()) {
            if (d.direction() == AlignmentDisplacementDirection.NATIVE || d.direction() == AlignmentDisplacementDirection.LATERAL) continue;
            byProfession.computeIfAbsent(d.declaredProfession(), ignored -> new LinkedHashMap<>())
                    .computeIfAbsent(d.alignmentId(), ignored -> new TreeSet<>()).add(d.compatibleProfession());
        }
        for (Map.Entry<String, Map<StructuralAlignmentId, Set<String>>> entry : byProfession.entrySet()) {
            Set<Set<String>> distinct = new HashSet<>(entry.getValue().values());
            if (distinct.size() > 1) {
                Set<String> targets = entry.getValue().values().stream().flatMap(Set::stream).collect(Collectors.toCollection(TreeSet::new));
                drafts.add(new ObservationDraft(EconomicHealthObservationType.PROFESSIONAL_DIVERGENCE, ObservationScope.PROFESSION,
                        EconomicHealthStatus.CONCERNING,
                        new EconomicHealthExplanation("Los sujetos observados de una misma profesión presentan destinos institucionales no nativos diferentes.", entry.getValue().keySet(), correlationsTouching(graph, entry.getValue().keySet()), Set.of(entry.getKey()), targets)));
            }
        }
    }

    private static void addMirrorMovement(EconomicCorrelationGraph graph, List<ObservationDraft> drafts) {
        for (EconomicCorrelation c : graph.correlations()) {
            if (!c.types().contains(EconomicCorrelationType.MIRROR_DISPLACEMENT)) continue;
            StructuralAlignment a = graph.alignments().get(c.firstAlignmentId());
            StructuralAlignment b = graph.alignments().get(c.secondAlignmentId());
            drafts.add(new ObservationDraft(EconomicHealthObservationType.MIRROR_MOVEMENT, ObservationScope.GROUP,
                    EconomicHealthStatus.CONCERNING,
                    new EconomicHealthExplanation("Dos alineamientos recorren el mismo puente institucional en sentidos opuestos. La complementariedad no prueba causalidad.", Set.of(a.id(), b.id()), Set.of(c.id()), Set.of(a.declaredProfession().name(), b.declaredProfession().name()), c.mirrorBridges())));
        }
    }

    private static void addSystemicAlignment(EconomicCorrelationGraph graph, List<ObservationDraft> drafts) {
        Set<String> common = null;
        for (StructuralAlignment a : graph.alignments().values()) {
            Set<String> profiles = a.compatibleProfiles().stream().map(p -> p.profile().profession()).collect(Collectors.toCollection(TreeSet::new));
            common = common == null ? profiles : intersection(common, profiles);
        }
        if (common != null && !common.isEmpty() && graph.alignments().size() > 1) {
            drafts.add(new ObservationDraft(EconomicHealthObservationType.SYSTEMIC_ALIGNMENT, ObservationScope.SYSTEM,
                    EconomicHealthStatus.CONCERNING,
                    new EconomicHealthExplanation("Todos los alineamientos de la estación comparten al menos un perfil institucional compatible.", graph.alignments().keySet(), graph.correlations().stream().map(EconomicCorrelation::id).collect(Collectors.toCollection(LinkedHashSet::new)), professions(graph.alignments().values()), common)));
        }
    }

    private static EconomicHealthStatus aggregate(List<EconomicHealthObservation> observations) {
        if (observations.stream().anyMatch(o -> o.indicator() == EconomicHealthStatus.CONCERNING)) return EconomicHealthStatus.CONCERNING;
        if (observations.stream().anyMatch(o -> o.indicator() == EconomicHealthStatus.UNKNOWN)) return EconomicHealthStatus.UNKNOWN;
        return EconomicHealthStatus.STABLE;
    }

    private static Map<StructuralAlignmentId, List<AlignmentDisplacement>> displacementIndex(EconomicCorrelationGraph graph) {
        return graph.displacements().stream().collect(Collectors.groupingBy(AlignmentDisplacement::alignmentId, LinkedHashMap::new, Collectors.toList()));
    }
    private static Set<String> targets(List<AlignmentDisplacement> ds) { return ds.stream().map(AlignmentDisplacement::compatibleProfession).collect(Collectors.toCollection(TreeSet::new)); }
    private static Set<String> professions(Collection<StructuralAlignment> values) { return values.stream().map(a -> a.declaredProfession().name()).collect(Collectors.toCollection(TreeSet::new)); }
    private static Set<String> intersection(Set<String> a, Set<String> b) { TreeSet<String> result = new TreeSet<>(a); result.retainAll(b); return result; }
    private static Set<EconomicCorrelationId> correlationsTouching(EconomicCorrelationGraph graph, Collection<StructuralAlignmentId> ids) {
        Set<StructuralAlignmentId> set = new HashSet<>(ids);
        return graph.correlations().stream().filter(c -> set.contains(c.firstAlignmentId()) || set.contains(c.secondAlignmentId())).map(EconomicCorrelation::id).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private record ObservationDraft(EconomicHealthObservationType type, ObservationScope scope, EconomicHealthStatus indicator, EconomicHealthExplanation explanation) {}
}
