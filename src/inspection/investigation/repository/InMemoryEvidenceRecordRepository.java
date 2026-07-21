package inspection.investigation.repository;

import inspection.casefile.InspectionCaseId;
import inspection.investigation.casefile.*;
import java.util.*;

public final class InMemoryEvidenceRecordRepository implements EvidenceRecordRepository {
    private final Map<EvidenceRecordId, EvidenceRecord> data = new LinkedHashMap<>();

    @Override public synchronized EvidenceRecord save(EvidenceRecord record) {
        Objects.requireNonNull(record);
        EvidenceRecord existing = data.putIfAbsent(record.id(), record);
        if (existing != null) throw new IllegalStateException("evidence record id already exists");
        return record;
    }
    @Override public synchronized Optional<EvidenceRecord> findById(EvidenceRecordId id) {
        return Optional.ofNullable(data.get(Objects.requireNonNull(id)));
    }
    @Override public synchronized List<EvidenceRecord> findByCaseId(InspectionCaseId caseId) {
        Objects.requireNonNull(caseId);
        return data.values().stream().filter(record -> record.inspectionCaseId().equals(caseId)).toList();
    }
    @Override public synchronized List<EvidenceRecord> findAll() { return List.copyOf(data.values()); }
    @Override public synchronized long count() { return data.size(); }
}
