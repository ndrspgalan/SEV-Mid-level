package inspection.investigation.repository;

import inspection.casefile.InspectionCaseId;
import inspection.investigation.casefile.*;
import java.util.*;

public final class InMemoryHypothesisRecordRepository implements HypothesisRecordRepository {
    private final Map<HypothesisRecordId, HypothesisRecord> data = new LinkedHashMap<>();

    @Override public synchronized HypothesisRecord save(HypothesisRecord record) {
        Objects.requireNonNull(record);
        HypothesisRecord existing = data.get(record.id());
        if (existing != null && !existing.inspectionCaseId().equals(record.inspectionCaseId())) {
            throw new IllegalStateException("hypothesis identity belongs to another inspection case");
        }
        data.put(record.id(), record);
        return record;
    }
    @Override public synchronized Optional<HypothesisRecord> findById(HypothesisRecordId id) {
        return Optional.ofNullable(data.get(Objects.requireNonNull(id)));
    }
    @Override public synchronized List<HypothesisRecord> findByCaseId(InspectionCaseId caseId) {
        Objects.requireNonNull(caseId);
        return data.values().stream().filter(record -> record.inspectionCaseId().equals(caseId)).toList();
    }
    @Override public synchronized List<HypothesisRecord> findAll() { return List.copyOf(data.values()); }
    @Override public synchronized long count() { return data.size(); }
}
