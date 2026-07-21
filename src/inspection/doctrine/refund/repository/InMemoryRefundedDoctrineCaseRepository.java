package inspection.doctrine.refund.repository;

import inspection.doctrine.refund.casefile.*;
import inspection.jurisprudence.casefile.JurisprudentialSimilarityKey;
import java.util.*;

public final class InMemoryRefundedDoctrineCaseRepository implements RefundedDoctrineCaseRepository {
    private final Map<RefundedDoctrineCaseId, RefundedDoctrineCase> data = new LinkedHashMap<>();

    @Override public synchronized RefundedDoctrineCase save(RefundedDoctrineCase doctrineCase) {
        Objects.requireNonNull(doctrineCase);
        RefundedDoctrineCase existing = data.get(doctrineCase.id());
        if (existing != null) {
            if (!existing.similarityKey().equals(doctrineCase.similarityKey())) {
                throw new IllegalStateException("refunded doctrine identity collision");
            }
            if (!doctrineCase.sourceUnifiedDoctrineCases().containsAll(existing.sourceUnifiedDoctrineCases())
                    || doctrineCase.doctrinalValue() < existing.doctrinalValue()) {
                throw new IllegalStateException("refunded doctrine evolution cannot lose historical sources or value");
            }
        }
        data.put(doctrineCase.id(), doctrineCase);
        return doctrineCase;
    }

    @Override public synchronized Optional<RefundedDoctrineCase> findById(RefundedDoctrineCaseId id) {
        return Optional.ofNullable(data.get(Objects.requireNonNull(id)));
    }
    @Override public synchronized Optional<RefundedDoctrineCase> findBySimilarityKey(JurisprudentialSimilarityKey key) {
        return findById(RefundedDoctrineCaseId.from(Objects.requireNonNull(key)));
    }
    @Override public synchronized List<RefundedDoctrineCase> findAll() { return List.copyOf(data.values()); }
    @Override public synchronized long count() { return data.size(); }
}
