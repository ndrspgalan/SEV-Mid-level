package inspection.jurisprudence.casefile;

import inspection.casefile.InspectionCaseId;
import java.util.Objects;

public record JurisprudentialComparisonId(String value) {
    public JurisprudentialComparisonId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("value");
    }

    public static JurisprudentialComparisonId from(InspectionCaseId caseId) {
        Objects.requireNonNull(caseId);
        return new JurisprudentialComparisonId("JCOMP|" + caseId.value());
    }
}
