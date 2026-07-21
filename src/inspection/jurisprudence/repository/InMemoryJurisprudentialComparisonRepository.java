package inspection.jurisprudence.repository;

import inspection.casefile.InspectionCaseId;
import inspection.jurisprudence.casefile.*;
import java.util.*;

public final class InMemoryJurisprudentialComparisonRepository implements JurisprudentialComparisonRepository {
    private final Map<JurisprudentialComparisonId, JurisprudentialComparison> data = new LinkedHashMap<>();

    @Override
    public synchronized JurisprudentialComparison save(JurisprudentialComparison comparison) {
        Objects.requireNonNull(comparison);
        JurisprudentialComparison existing = data.get(comparison.id());
        if (existing != null) {
            if (existing != comparison) throw new IllegalStateException("inspection case already has a jurisprudential comparison");
            return existing;
        }
        data.put(comparison.id(), comparison);
        return comparison;
    }

    @Override
    public synchronized Optional<JurisprudentialComparison> findById(JurisprudentialComparisonId id) {
        return Optional.ofNullable(data.get(Objects.requireNonNull(id)));
    }

    @Override
    public synchronized Optional<JurisprudentialComparison> findByCaseId(InspectionCaseId caseId) {
        return findById(JurisprudentialComparisonId.from(caseId));
    }

    @Override
    public synchronized List<JurisprudentialComparison> findAll() { return List.copyOf(data.values()); }

    @Override
    public synchronized long count() { return data.size(); }
}
