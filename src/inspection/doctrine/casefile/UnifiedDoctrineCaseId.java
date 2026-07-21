package inspection.doctrine.casefile;

import behavior.temporal.SeasonPeriod;
import inspection.jurisprudence.casefile.JurisprudentialSimilarityKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

public record UnifiedDoctrineCaseId(String value) {
    public UnifiedDoctrineCaseId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("blank unified doctrine case id");
    }

    public static UnifiedDoctrineCaseId from(SeasonPeriod season, JurisprudentialSimilarityKey key) {
        Objects.requireNonNull(season); Objects.requireNonNull(key);
        String canonical = season.label() + "|" + key.origin() + "|" + key.financialReasons()
                + "|" + key.professions() + "|" + key.institutionalProfiles();
        UUID stable = UUID.nameUUIDFromBytes(canonical.getBytes(StandardCharsets.UTF_8));
        return new UnifiedDoctrineCaseId("UDOC|" + season.label() + "|" + stable);
    }
}
