package inspection.repository;

import inspection.casefile.*;
import java.util.*;

/** Persistent M4 repository. Cases are appended, never rebuilt from analytical projections. */
public interface InspectionCaseRepository {
    InspectionCase save(InspectionCase inspectionCase);
    Optional<InspectionCase> findById(InspectionCaseId id);
    List<InspectionCase> findAll();
    long count();
}
