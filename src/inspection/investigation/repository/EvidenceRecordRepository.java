package inspection.investigation.repository;

import inspection.casefile.InspectionCaseId;
import inspection.investigation.casefile.*;
import java.util.*;

public interface EvidenceRecordRepository {
    EvidenceRecord save(EvidenceRecord record);
    Optional<EvidenceRecord> findById(EvidenceRecordId id);
    List<EvidenceRecord> findByCaseId(InspectionCaseId caseId);
    List<EvidenceRecord> findAll();
    long count();
}
