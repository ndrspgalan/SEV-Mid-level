package inspection.investigation.repository;

import inspection.casefile.InspectionCaseId;
import inspection.investigation.casefile.*;
import java.util.*;

public interface HypothesisRecordRepository {
    HypothesisRecord save(HypothesisRecord record);
    Optional<HypothesisRecord> findById(HypothesisRecordId id);
    List<HypothesisRecord> findByCaseId(InspectionCaseId caseId);
    List<HypothesisRecord> findAll();
    long count();
}
