package inspection.resolution.casefile;

import inspection.investigation.casefile.*;
import java.util.*;

/**
 * Human-authored rationale and traceability selected by the inspector.
 * M4.3 never decides the resolution automatically and never converts evidence
 * or hypotheses into guilt. Judicial use of this record belongs to institutions
 * outside SEV.
 */
public final class InspectionResolutionExplanation {
    private final String rationale;
    private final Set<EvidenceRecordId> supportingEvidence;
    private final Set<HypothesisRecordId> consideredHypotheses;

    public InspectionResolutionExplanation(
            String rationale,
            Collection<EvidenceRecordId> supportingEvidence,
            Collection<HypothesisRecordId> consideredHypotheses
    ) {
        this.rationale = requireText(rationale);
        this.supportingEvidence = immutableNonNullSet(supportingEvidence, "supporting evidence");
        this.consideredHypotheses = immutableNonNullSet(consideredHypotheses, "considered hypotheses");
    }

    private static String requireText(String value) {
        Objects.requireNonNull(value, "resolution rationale must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("resolution rationale must not be blank");
        return normalized;
    }

    private static <T> Set<T> immutableNonNullSet(Collection<T> values, String label) {
        Objects.requireNonNull(values, label + " must not be null");
        LinkedHashSet<T> copy = new LinkedHashSet<>();
        for (T value : values) copy.add(Objects.requireNonNull(value, label + " must not contain null"));
        return Collections.unmodifiableSet(copy);
    }

    public String rationale() { return rationale; }
    public Set<EvidenceRecordId> supportingEvidence() { return supportingEvidence; }
    public Set<HypothesisRecordId> consideredHypotheses() { return consideredHypotheses; }
}
