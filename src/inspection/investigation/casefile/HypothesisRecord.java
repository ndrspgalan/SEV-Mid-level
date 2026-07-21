package inspection.investigation.casefile;

import inspection.casefile.InspectionCaseId;
import java.time.Instant;
import java.util.*;

/**
 * A hypothesis formulated by the inspector and contrasted against evidence
 * belonging to the same case.
 */
public final class HypothesisRecord {
    private final HypothesisRecordId id;
    private final InspectionCaseId inspectionCaseId;
    private final String statement;
    private final HypothesisRecordStatus status;
    private final Map<EvidenceRecordId, HypothesisEvidenceImpact> evidence;
    private final Instant createdAt;
    private final Instant updatedAt;

    private HypothesisRecord(
            HypothesisRecordId id,
            InspectionCaseId inspectionCaseId,
            String statement,
            HypothesisRecordStatus status,
            Map<EvidenceRecordId, HypothesisEvidenceImpact> evidence,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.inspectionCaseId = Objects.requireNonNull(inspectionCaseId);
        this.statement = requireText(statement);
        this.status = Objects.requireNonNull(status);
        this.evidence = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(evidence)));
        this.createdAt = Objects.requireNonNull(createdAt);
        this.updatedAt = Objects.requireNonNull(updatedAt);
        if (updatedAt.isBefore(createdAt)) throw new IllegalArgumentException("hypothesis update cannot precede creation");
        this.evidence.forEach((evidenceId, impact) -> {
            Objects.requireNonNull(evidenceId, "evidence id must not be null");
            Objects.requireNonNull(impact, "evidence impact must not be null");
        });
    }

    public static HypothesisRecord open(
            HypothesisRecordId id,
            InspectionCaseId inspectionCaseId,
            String statement,
            Instant createdAt
    ) {
        return new HypothesisRecord(id, inspectionCaseId, statement, HypothesisRecordStatus.OPEN,
                Map.of(), createdAt, createdAt);
    }

    public HypothesisRecord relate(
            EvidenceRecordId evidenceRecordId,
            HypothesisEvidenceImpact impact,
            Instant updatedAt
    ) {
        Map<EvidenceRecordId, HypothesisEvidenceImpact> updated = new LinkedHashMap<>(evidence);
        updated.put(Objects.requireNonNull(evidenceRecordId), Objects.requireNonNull(impact));
        return new HypothesisRecord(id, inspectionCaseId, statement, status, updated, createdAt, updatedAt);
    }

    public HypothesisRecord conclude(HypothesisRecordStatus conclusion, Instant updatedAt) {
        Objects.requireNonNull(conclusion);
        if (conclusion == HypothesisRecordStatus.OPEN) {
            throw new IllegalArgumentException("OPEN is not a hypothesis conclusion");
        }
        return new HypothesisRecord(id, inspectionCaseId, statement, conclusion, evidence, createdAt, updatedAt);
    }

    private static String requireText(String value) {
        Objects.requireNonNull(value, "hypothesis statement must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException("hypothesis statement must not be blank");
        return normalized;
    }

    public HypothesisRecordId id() { return id; }
    public InspectionCaseId inspectionCaseId() { return inspectionCaseId; }
    public String statement() { return statement; }
    public HypothesisRecordStatus status() { return status; }
    public Map<EvidenceRecordId, HypothesisEvidenceImpact> evidence() { return evidence; }
    public Set<EvidenceRecordId> supportingEvidence() { return idsWith(HypothesisEvidenceImpact.SUPPORTS); }
    public Set<EvidenceRecordId> refutingEvidence() { return idsWith(HypothesisEvidenceImpact.REFUTES); }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    private Set<EvidenceRecordId> idsWith(HypothesisEvidenceImpact impact) {
        LinkedHashSet<EvidenceRecordId> ids = new LinkedHashSet<>();
        evidence.forEach((id, value) -> { if (value == impact) ids.add(id); });
        return Collections.unmodifiableSet(ids);
    }
}
