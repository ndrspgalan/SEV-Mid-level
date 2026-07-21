package inspection.doctrine.casefile;

import behavior.temporal.SeasonPeriod;
import inspection.casefile.InspectionCaseId;
import inspection.jurisprudence.casefile.JurisprudentialSimilarityKey;
import inspection.resolution.casefile.InspectionResolutionType;
import java.time.Instant;
import java.util.*;

/** Immutable seasonal doctrine created from at least two univocally similar closed cases. */
public record UnifiedDoctrineCase(
        UnifiedDoctrineCaseId id,
        SeasonPeriod seasonPeriod,
        JurisprudentialSimilarityKey similarityKey,
        InspectionResolutionType unifiedResolution,
        JurisprudentialConsensusType consensusType,
        DoctrineUnificationMethod unificationMethod,
        int doctrinalValue,
        Set<InspectionCaseId> sourceInspectionCases,
        Instant unifiedAt
) {
    public UnifiedDoctrineCase {
        Objects.requireNonNull(id); Objects.requireNonNull(seasonPeriod); Objects.requireNonNull(similarityKey);
        Objects.requireNonNull(unifiedResolution); Objects.requireNonNull(consensusType);
        Objects.requireNonNull(unificationMethod); Objects.requireNonNull(unifiedAt);
        sourceInspectionCases = Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(sourceInspectionCases)));
        if (!id.equals(UnifiedDoctrineCaseId.from(seasonPeriod, similarityKey))) {
            throw new IllegalArgumentException("unified doctrine case identity mismatch");
        }
        if (sourceInspectionCases.size() < 2) {
            throw new IllegalArgumentException("seasonal doctrine requires at least two source inspection cases");
        }
        if (doctrinalValue != sourceInspectionCases.size()) {
            throw new IllegalArgumentException("doctrinal value must equal the number of unified cases");
        }
        DoctrineUnificationMethod expected = switch (consensusType) {
            case ABSOLUTE_AGREEMENT, RELATIVE_AGREEMENT -> DoctrineUnificationMethod.DOMINANT_AGREEMENT;
            case NULL_AGREEMENT, ABSOLUTE_DISAGREEMENT -> DoctrineUnificationMethod.FORCED_MODE;
        };
        if (unificationMethod != expected) throw new IllegalArgumentException("doctrine unification method mismatch");
    }
}
