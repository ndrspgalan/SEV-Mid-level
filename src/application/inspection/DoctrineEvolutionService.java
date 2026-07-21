package application.inspection;

import inspection.doctrine.casefile.UnifiedDoctrineCase;
import inspection.doctrine.repository.UnifiedDoctrineCaseRepository;
import inspection.doctrine.refund.casefile.*;
import inspection.doctrine.refund.repository.RefundedDoctrineCaseRepository;
import inspection.jurisprudence.casefile.JurisprudentialSimilarityKey;
import inspection.resolution.casefile.InspectionResolutionType;
import java.time.Clock;
import java.util.*;

/** M5.3 refunds seasonal doctrine into one evolving longitudinal doctrine per financial family. */
public final class DoctrineEvolutionService {
    private final UnifiedDoctrineCaseRepository seasonalDoctrines;
    private final RefundedDoctrineCaseRepository refundedDoctrines;
    private final Clock clock;

    public DoctrineEvolutionService(
            UnifiedDoctrineCaseRepository seasonalDoctrines,
            RefundedDoctrineCaseRepository refundedDoctrines,
            Clock clock
    ) {
        this.seasonalDoctrines = Objects.requireNonNull(seasonalDoctrines);
        this.refundedDoctrines = Objects.requireNonNull(refundedDoctrines);
        this.clock = Objects.requireNonNull(clock);
    }

    /** Rebuilds every longitudinal family from the complete immutable seasonal doctrine corpus. */
    public List<RefundedDoctrineCase> refundAll() {
        Map<JurisprudentialSimilarityKey, List<UnifiedDoctrineCase>> families = new LinkedHashMap<>();
        for (UnifiedDoctrineCase doctrine : seasonalDoctrines.findAll()) {
            families.computeIfAbsent(doctrine.similarityKey(), ignored -> new ArrayList<>()).add(doctrine);
        }
        List<RefundedDoctrineCase> result = new ArrayList<>();
        for (Map.Entry<JurisprudentialSimilarityKey, List<UnifiedDoctrineCase>> entry : families.entrySet()) {
            result.add(refundFamily(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(result);
    }

    public Optional<RefundedDoctrineCase> refund(JurisprudentialSimilarityKey key) {
        Objects.requireNonNull(key);
        List<UnifiedDoctrineCase> family = seasonalDoctrines.findAll().stream()
                .filter(value -> value.similarityKey().equals(key)).toList();
        return family.isEmpty() ? Optional.empty() : Optional.of(refundFamily(key, family));
    }

    public Optional<RefundedDoctrineCase> findBySimilarityKey(JurisprudentialSimilarityKey key) {
        return refundedDoctrines.findBySimilarityKey(Objects.requireNonNull(key));
    }
    public List<RefundedDoctrineCase> findAll() { return refundedDoctrines.findAll(); }
    public long count() { return refundedDoctrines.count(); }

    private RefundedDoctrineCase refundFamily(
            JurisprudentialSimilarityKey key,
            List<UnifiedDoctrineCase> family
    ) {
        List<UnifiedDoctrineCase> ordered = family.stream()
                .sorted(Comparator.comparing(value -> value.seasonPeriod().startsOn()))
                .toList();
        EnumMap<InspectionResolutionType, Integer> weights = new EnumMap<>(InspectionResolutionType.class);
        LinkedHashSet<behavior.temporal.SeasonPeriod> seasons = new LinkedHashSet<>();
        LinkedHashSet<inspection.doctrine.casefile.UnifiedDoctrineCaseId> sourceIds = new LinkedHashSet<>();
        int totalValue = 0;

        for (UnifiedDoctrineCase doctrine : ordered) {
            if (!doctrine.similarityKey().equals(key)) {
                throw new IllegalStateException("seasonal doctrine does not belong to the refunded family");
            }
            if (!seasons.add(doctrine.seasonPeriod())) {
                throw new IllegalStateException("more than one seasonal doctrine exists for the same family and Season");
            }
            sourceIds.add(doctrine.id());
            weights.merge(doctrine.unifiedResolution(), doctrine.doctrinalValue(), Integer::sum);
            totalValue = Math.addExact(totalValue, doctrine.doctrinalValue());
        }

        Optional<InspectionResolutionType> winner = uniqueWeightedLeader(weights);
        RefundedDoctrineOutcome outcome = winner
                .map(value -> RefundedDoctrineOutcome.valueOf(value.name()))
                .orElse(RefundedDoctrineOutcome.DOCTRINAL_CONFLICT);
        RefundedDoctrineCase refunded = new RefundedDoctrineCase(
                RefundedDoctrineCaseId.from(key), key, outcome, winner, totalValue,
                seasons, sourceIds, clock.instant());
        return refundedDoctrines.save(refunded);
    }

    private static Optional<InspectionResolutionType> uniqueWeightedLeader(
            EnumMap<InspectionResolutionType, Integer> weights
    ) {
        int maximum = weights.values().stream().mapToInt(Integer::intValue).max().orElseThrow();
        List<InspectionResolutionType> leaders = weights.entrySet().stream()
                .filter(entry -> entry.getValue() == maximum)
                .map(Map.Entry::getKey)
                .toList();
        return leaders.size() == 1 ? Optional.of(leaders.get(0)) : Optional.empty();
    }
}
