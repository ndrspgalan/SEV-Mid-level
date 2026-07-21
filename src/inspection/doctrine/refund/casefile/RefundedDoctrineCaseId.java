package inspection.doctrine.refund.casefile;

import inspection.jurisprudence.casefile.JurisprudentialSimilarityKey;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/** Stable identity of one longitudinal doctrine family. */
public record RefundedDoctrineCaseId(String value) {
    public RefundedDoctrineCaseId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("blank refunded doctrine case id");
    }

    public static RefundedDoctrineCaseId from(JurisprudentialSimilarityKey key) {
        Objects.requireNonNull(key);
        String canonical = key.origin() + "|" + key.financialReasons()
                + "|" + key.professions() + "|" + key.institutionalProfiles();
        UUID stable = UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
        return new RefundedDoctrineCaseId("RDOC|" + stable);
    }
}
