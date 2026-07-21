package inspection.investigation.casefile;

import java.util.Objects;
import java.util.UUID;

public record HypothesisRecordId(UUID value) {
    public HypothesisRecordId { Objects.requireNonNull(value, "hypothesis record id must not be null"); }
    public static HypothesisRecordId generate() { return new HypothesisRecordId(UUID.randomUUID()); }
    @Override public String toString() { return value.toString(); }
}
