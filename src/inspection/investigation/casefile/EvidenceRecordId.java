package inspection.investigation.casefile;

import java.util.Objects;
import java.util.UUID;

public record EvidenceRecordId(UUID value) {
    public EvidenceRecordId { Objects.requireNonNull(value, "evidence record id must not be null"); }
    public static EvidenceRecordId generate() { return new EvidenceRecordId(UUID.randomUUID()); }
    @Override public String toString() { return value.toString(); }
}
