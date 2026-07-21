package behavior.correlation.analysis;

import behavior.alignment.profile.*;
import behavior.correlation.profile.*;
import behavior.temporal.SeasonPeriod;
import operationalControl.profile.ProfessionCreditProfile;
import java.util.*;

/** Builds exhaustive descriptive M3.5 graphs without health, inspection or fraud semantics. */
public final class EconomicCorrelationAnalyzer {

    public EconomicCorrelationAnalysisReport analyze(
            Collection<StructuralAlignment> alignments,
            Collection<ProfessionCreditProfile> institutionalProfiles
    ) {
        Objects.requireNonNull(alignments); Objects.requireNonNull(institutionalProfiles);
        InstitutionalProfileTopology topology = new InstitutionalProfileTopology(
                institutionalProfiles.stream().map(CreditProfileDescriptor::from).toList());

        Map<SeasonPeriod, List<StructuralAlignment>> bySeason = new TreeMap<>(
                Comparator.comparing(SeasonPeriod::startsOn).thenComparing(SeasonPeriod::endsOn));
        for (StructuralAlignment alignment : alignments) {
            bySeason.computeIfAbsent(alignment.seasonPeriod(), ignored -> new ArrayList<>()).add(alignment);
        }

        List<EconomicCorrelationGraph> graphs = new ArrayList<>();
        long pairs = 0, correlations = 0, mirrors = 0, clusters = 0;
        for (Map.Entry<SeasonPeriod, List<StructuralAlignment>> entry : bySeason.entrySet()) {
            List<StructuralAlignment> seasonAlignments = entry.getValue().stream()
                    .sorted(Comparator.comparing(a -> a.id().value())).toList();
            Map<StructuralAlignmentId, List<AlignmentDisplacement>> displacementIndex = new LinkedHashMap<>();
            List<AlignmentDisplacement> allDisplacements = new ArrayList<>();
            for (StructuralAlignment alignment : seasonAlignments) {
                List<AlignmentDisplacement> values = displacements(alignment, topology);
                displacementIndex.put(alignment.id(), values);
                allDisplacements.addAll(values);
            }

            List<EconomicCorrelation> edges = new ArrayList<>();
            for (int i = 0; i < seasonAlignments.size(); i++) {
                for (int j = i + 1; j < seasonAlignments.size(); j++) {
                    pairs++;
                    Optional<EconomicCorrelation> edge = correlate(
                            seasonAlignments.get(i), seasonAlignments.get(j),
                            displacementIndex.get(seasonAlignments.get(i).id()),
                            displacementIndex.get(seasonAlignments.get(j).id()));
                    if (edge.isPresent()) {
                        edges.add(edge.get());
                        correlations++;
                        if (edge.get().types().contains(EconomicCorrelationType.MIRROR_DISPLACEMENT)) mirrors++;
                    }
                }
            }

            List<EconomicCorrelationCluster> graphClusters = clusters(entry.getKey(), seasonAlignments, edges);
            clusters += graphClusters.size();
            LinkedHashMap<StructuralAlignmentId, StructuralAlignment> nodeMap = new LinkedHashMap<>();
            for (StructuralAlignment alignment : seasonAlignments) nodeMap.put(alignment.id(), alignment);
            graphs.add(new EconomicCorrelationGraph(
                    EconomicCorrelationGraphId.from(entry.getKey()), entry.getKey(), nodeMap,
                    allDisplacements, edges, graphClusters));
        }
        return new EconomicCorrelationAnalysisReport(
                alignments.size(), bySeason.size(), pairs, correlations, mirrors, clusters, graphs);
    }

    private static List<AlignmentDisplacement> displacements(
            StructuralAlignment alignment, InstitutionalProfileTopology topology) {
        List<AlignmentDisplacement> result = new ArrayList<>();
        for (ProfileCompatibility compatible : alignment.compatibleProfiles()) {
            String declared = alignment.declaredProfession().name();
            String target = compatible.profile().profession();
            InstitutionalProfileRelation relation = topology.relation(declared, target);
            AlignmentDisplacementDirection direction;
            if (declared.equals(target)) direction = AlignmentDisplacementDirection.NATIVE;
            else direction = switch (relation) {
                case EQUIVALENT -> AlignmentDisplacementDirection.LATERAL;
                case MORE_EXPANSIVE -> AlignmentDisplacementDirection.UPWARD;
                case MORE_RESTRICTIVE -> AlignmentDisplacementDirection.DOWNWARD;
                case CROSS_PROFILE -> AlignmentDisplacementDirection.CROSS_PROFILE;
            };
            result.add(new AlignmentDisplacement(alignment.id(), declared, target, relation, direction));
        }
        return result.stream()
                .sorted(Comparator.comparing(AlignmentDisplacement::compatibleProfession))
                .toList();
    }

    private static Optional<EconomicCorrelation> correlate(
            StructuralAlignment first,
            StructuralAlignment second,
            List<AlignmentDisplacement> firstDisplacements,
            List<AlignmentDisplacement> secondDisplacements
    ) {
        EnumSet<EconomicCorrelationType> types = EnumSet.noneOf(EconomicCorrelationType.class);
        if (first.declaredProfession().equals(second.declaredProfession())) {
            types.add(EconomicCorrelationType.SAME_DECLARED_PROFESSION);
        }

        Set<String> firstCompatible = compatibleNames(first);
        Set<String> secondCompatible = compatibleNames(second);
        Set<String> sharedCompatible = intersection(firstCompatible, secondCompatible);
        if (!sharedCompatible.isEmpty()) types.add(EconomicCorrelationType.SHARED_COMPATIBLE_PROFILE);

        boolean reciprocal = firstCompatible.contains(second.declaredProfession().name())
                && secondCompatible.contains(first.declaredProfession().name());
        if (reciprocal) types.add(EconomicCorrelationType.RECIPROCAL_PROFILE_COMPATIBILITY);

        Set<String> firstTargets = nonNativeTargets(firstDisplacements);
        Set<String> secondTargets = nonNativeTargets(secondDisplacements);
        Set<String> sharedTargets = intersection(firstTargets, secondTargets);
        if (!sharedTargets.isEmpty()) types.add(EconomicCorrelationType.SHARED_DISPLACEMENT_TARGET);

        Set<String> mirrorBridges = mirrorBridges(firstDisplacements, secondDisplacements);
        if (!mirrorBridges.isEmpty()) types.add(EconomicCorrelationType.MIRROR_DISPLACEMENT);

        if (types.isEmpty()) return Optional.empty();
        return Optional.of(new EconomicCorrelation(
                EconomicCorrelationId.between(first.id(), second.id()), first.id(), second.id(),
                first.seasonPeriod(), types, sharedCompatible, sharedTargets, mirrorBridges));
    }

    private static Set<String> compatibleNames(StructuralAlignment alignment) {
        TreeSet<String> names = new TreeSet<>();
        alignment.compatibleProfiles().forEach(p -> names.add(p.profile().profession()));
        return names;
    }

    private static Set<String> nonNativeTargets(List<AlignmentDisplacement> displacements) {
        TreeSet<String> names = new TreeSet<>();
        displacements.stream().filter(d -> d.direction() != AlignmentDisplacementDirection.NATIVE)
                .forEach(d -> names.add(d.compatibleProfession()));
        return names;
    }

    private static Set<String> mirrorBridges(
            List<AlignmentDisplacement> first,
            List<AlignmentDisplacement> second) {
        TreeSet<String> bridges = new TreeSet<>();
        for (AlignmentDisplacement a : first) {
            if (!directional(a.direction())) continue;
            for (AlignmentDisplacement b : second) {
                if (!directional(b.direction())) continue;
                boolean reversed = a.declaredProfession().equals(b.compatibleProfession())
                        && a.compatibleProfession().equals(b.declaredProfession());
                boolean opposite = (a.direction() == AlignmentDisplacementDirection.UPWARD
                        && b.direction() == AlignmentDisplacementDirection.DOWNWARD)
                        || (a.direction() == AlignmentDisplacementDirection.DOWNWARD
                        && b.direction() == AlignmentDisplacementDirection.UPWARD);
                if (reversed && opposite) bridges.add(a.bridgeKey());
            }
        }
        return bridges;
    }

    private static boolean directional(AlignmentDisplacementDirection direction) {
        return direction == AlignmentDisplacementDirection.UPWARD
                || direction == AlignmentDisplacementDirection.DOWNWARD;
    }

    private static Set<String> intersection(Set<String> first, Set<String> second) {
        TreeSet<String> result = new TreeSet<>(first);
        result.retainAll(second);
        return result;
    }

    private static List<EconomicCorrelationCluster> clusters(
            SeasonPeriod period,
            List<StructuralAlignment> alignments,
            List<EconomicCorrelation> correlations) {
        Map<StructuralAlignmentId, Set<StructuralAlignmentId>> adjacency = new LinkedHashMap<>();
        for (StructuralAlignment alignment : alignments) adjacency.put(alignment.id(), new LinkedHashSet<>());
        for (EconomicCorrelation edge : correlations) {
            adjacency.get(edge.firstAlignmentId()).add(edge.secondAlignmentId());
            adjacency.get(edge.secondAlignmentId()).add(edge.firstAlignmentId());
        }

        Set<StructuralAlignmentId> visited = new HashSet<>();
        List<EconomicCorrelationCluster> result = new ArrayList<>();
        int ordinal = 1;
        for (StructuralAlignmentId start : adjacency.keySet()) {
            if (!visited.add(start)) continue;
            LinkedHashSet<StructuralAlignmentId> nodes = new LinkedHashSet<>();
            ArrayDeque<StructuralAlignmentId> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                StructuralAlignmentId current = queue.removeFirst();
                nodes.add(current);
                for (StructuralAlignmentId next : adjacency.get(current)) {
                    if (visited.add(next)) queue.addLast(next);
                }
            }
            LinkedHashSet<EconomicCorrelationId> edgeIds = new LinkedHashSet<>();
            for (EconomicCorrelation edge : correlations) {
                if (nodes.contains(edge.firstAlignmentId()) && nodes.contains(edge.secondAlignmentId())) edgeIds.add(edge.id());
            }
            String number = String.format("%03d", ordinal++);
            result.add(new EconomicCorrelationCluster(
                    new EconomicCorrelationClusterId("CLUSTER|" + period.label() + "|" + number), nodes, edgeIds));
        }
        return result;
    }
}
