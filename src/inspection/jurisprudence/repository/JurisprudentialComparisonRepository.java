package inspection.jurisprudence.repository;

import inspection.casefile.InspectionCaseId;
import inspection.jurisprudence.casefile.*;
import java.util.*;

/** Append-only record of the jurisprudence available when each case was closed. */
public interface JurisprudentialComparisonRepository {
    JurisprudentialComparison save(JurisprudentialComparison comparison);
    Optional<JurisprudentialComparison> findById(JurisprudentialComparisonId id);
    Optional<JurisprudentialComparison> findByCaseId(InspectionCaseId caseId);
    List<JurisprudentialComparison> findAll();
    long count();
}
