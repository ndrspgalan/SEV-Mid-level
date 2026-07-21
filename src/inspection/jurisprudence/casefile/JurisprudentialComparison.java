package inspection.jurisprudence.casefile;

import behavior.temporal.SeasonPeriod;
import inspection.casefile.InspectionCaseId;
import inspection.resolution.casefile.InspectionResolutionType;
import java.time.Instant;
import java.util.*;

/** Immutable comparison performed immediately before the inspection case is closed. */
public record JurisprudentialComparison(
        JurisprudentialComparisonId id,
        InspectionCaseId inspectionCaseId,
        SeasonPeriod seasonPeriod,
        JurisprudentialSimilarityKey similarityKey,
        Set<InspectionCaseId> comparedCases,
        InspectionResolutionType currentResolution,
        Optional<InspectionResolutionType> dominantResolution,
        JurisprudentialAgreement agreement,
        Instant comparedAt
) {
    public JurisprudentialComparison {
        Objects.requireNonNull(id); Objects.requireNonNull(inspectionCaseId); Objects.requireNonNull(seasonPeriod);
        Objects.requireNonNull(similarityKey); Objects.requireNonNull(currentResolution);
        Objects.requireNonNull(dominantResolution); Objects.requireNonNull(agreement); Objects.requireNonNull(comparedAt);
        if (!id.equals(JurisprudentialComparisonId.from(inspectionCaseId))) {
            throw new IllegalArgumentException("jurisprudential comparison identity mismatch");
        }
        comparedCases = Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(comparedCases)));
        if (comparedCases.contains(inspectionCaseId)) {
            throw new IllegalArgumentException("a case cannot be compared with itself");
        }
        if (dominantResolution.isEmpty() && agreement != JurisprudentialAgreement.NO_CONSTA) {
            throw new IllegalArgumentException("absence of dominant jurisprudence requires NO_CONSTA");
        }
        if (dominantResolution.isPresent()) {
            JurisprudentialAgreement expected = dominantResolution.get() == currentResolution
                    ? JurisprudentialAgreement.YES : JurisprudentialAgreement.NO;
            if (agreement != expected) throw new IllegalArgumentException("jurisprudential agreement mismatch");
        }
    }

    public boolean jurisprudenceExists() { return dominantResolution.isPresent(); }
}
