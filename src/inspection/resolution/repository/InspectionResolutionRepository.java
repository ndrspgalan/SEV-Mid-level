package inspection.resolution.repository;

import inspection.casefile.InspectionCaseId;
import inspection.resolution.casefile.*;
import java.util.*;

/** Append-only institutional memory of final inspection resolutions. */
public interface InspectionResolutionRepository {
    InspectionResolution save(InspectionResolution resolution);
    Optional<InspectionResolution> findById(InspectionResolutionId id);
    Optional<InspectionResolution> findByCaseId(InspectionCaseId caseId);
    List<InspectionResolution> findAll();
    long count();
}
