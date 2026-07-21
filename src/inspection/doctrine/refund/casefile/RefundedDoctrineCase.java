package inspection.doctrine.refund.casefile;

import behavior.temporal.SeasonPeriod;
import inspection.doctrine.casefile.UnifiedDoctrineCaseId;
import inspection.jurisprudence.casefile.JurisprudentialSimilarityKey;
import inspection.resolution.casefile.InspectionResolutionType;
import java.time.Instant;
import java.util.*;

/** Immutable longitudinal refund of every seasonal doctrine belonging to one financial family. */
public record RefundedDoctrineCase(
        RefundedDoctrineCaseId id,
        JurisprudentialSimilarityKey similarityKey,
        RefundedDoctrineOutcome currentOutcome,
        Optional<InspectionResolutionType> currentResolution,
        int doctrinalValue,
        Set<SeasonPeriod> coveredSeasons,
        Set<UnifiedDoctrineCaseId> sourceUnifiedDoctrineCases,
        Instant refundedAt
) {
    public RefundedDoctrineCase {
        Objects.requireNonNull(id); Objects.requireNonNull(similarityKey); Objects.requireNonNull(currentOutcome);
        currentResolution = Objects.requireNonNull(currentResolution);
        Objects.requireNonNull(refundedAt);
        coveredSeasons = immutableSeasons(coveredSeasons);
        sourceUnifiedDoctrineCases = Collections.unmodifiableSet(
                new LinkedHashSet<>(Objects.requireNonNull(sourceUnifiedDoctrineCases)));
        if (!id.equals(RefundedDoctrineCaseId.from(similarityKey))) {
            throw new IllegalArgumentException("refunded doctrine case identity mismatch");
        }
        if (sourceUnifiedDoctrineCases.isEmpty()) {
            throw new IllegalArgumentException("refunded doctrine requires at least one seasonal doctrine");
        }
        if (coveredSeasons.size() != sourceUnifiedDoctrineCases.size()) {
            throw new IllegalArgumentException("every source seasonal doctrine must contribute one covered Season");
        }
        if (doctrinalValue <= 0) throw new IllegalArgumentException("doctrinal value must be positive");
        if (currentOutcome == RefundedDoctrineOutcome.DOCTRINAL_CONFLICT) {
            if (currentResolution.isPresent()) {
                throw new IllegalArgumentException("a doctrinal conflict cannot expose a current resolution");
            }
        } else {
            InspectionResolutionType expected = InspectionResolutionType.valueOf(currentOutcome.name());
            if (currentResolution.isEmpty() || currentResolution.get() != expected) {
                throw new IllegalArgumentException("refunded doctrine outcome and current resolution mismatch");
            }
        }
    }

    public boolean hasConflict() { return currentOutcome == RefundedDoctrineOutcome.DOCTRINAL_CONFLICT; }

    private static Set<SeasonPeriod> immutableSeasons(Set<SeasonPeriod> values) {
        List<SeasonPeriod> sorted = new ArrayList<>(Objects.requireNonNull(values));
        sorted.sort(Comparator.comparing(SeasonPeriod::startsOn).thenComparing(SeasonPeriod::endsOn));
        return Collections.unmodifiableSet(new LinkedHashSet<>(sorted));
    }
}
