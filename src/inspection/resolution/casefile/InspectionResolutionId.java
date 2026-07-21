package inspection.resolution.casefile;

import inspection.casefile.InspectionCaseId;
import java.util.Objects;

/** One deterministic resolution identity per inspection case. */
public record InspectionResolutionId(String value) {
    public InspectionResolutionId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("value");
    }

    public static InspectionResolutionId from(InspectionCaseId caseId) {
        return new InspectionResolutionId("INSPECTION-RESOLUTION|" + Objects.requireNonNull(caseId).value());
    }
}
