package inspection.investigation.casefile;

import inspection.casefile.InspectionCaseId;
import java.time.Instant;
import java.util.Objects;

/**
 * Evidence manually incorporated into an open inspection case.
 * M4.2 records the inspector's work; it does not generate evidence.
 */
public final class EvidenceRecord {
    private final EvidenceRecordId id;
    private final InspectionCaseId inspectionCaseId;
    private final EvidenceRecordType type;
    private final String title;
    private final String description;
    private final String sourceReference;
    private final Instant obtainedAt;
    private final Instant registeredAt;

    public EvidenceRecord(
            EvidenceRecordId id,
            InspectionCaseId inspectionCaseId,
            EvidenceRecordType type,
            String title,
            String description,
            String sourceReference,
            Instant obtainedAt,
            Instant registeredAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.inspectionCaseId = Objects.requireNonNull(inspectionCaseId);
        this.type = Objects.requireNonNull(type);
        this.title = requireText(title, "evidence title");
        this.description = requireText(description, "evidence description");
        this.sourceReference = requireText(sourceReference, "evidence source reference");
        this.obtainedAt = Objects.requireNonNull(obtainedAt);
        this.registeredAt = Objects.requireNonNull(registeredAt);
        if (obtainedAt.isAfter(registeredAt)) {
            throw new IllegalArgumentException("evidence cannot be obtained after it is registered");
        }
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return normalized;
    }

    public EvidenceRecordId id() { return id; }
    public InspectionCaseId inspectionCaseId() { return inspectionCaseId; }
    public EvidenceRecordType type() { return type; }
    public String title() { return title; }
    public String description() { return description; }
    public String sourceReference() { return sourceReference; }
    public Instant obtainedAt() { return obtainedAt; }
    public Instant registeredAt() { return registeredAt; }
}
