package inspection.doctrine.repository;

import behavior.temporal.SeasonPeriod;
import inspection.doctrine.casefile.*;
import inspection.jurisprudence.casefile.JurisprudentialSimilarityKey;
import java.util.*;

public final class InMemoryUnifiedDoctrineCaseRepository implements UnifiedDoctrineCaseRepository {
    private final Map<UnifiedDoctrineCaseId, UnifiedDoctrineCase> data = new LinkedHashMap<>();

    @Override public synchronized UnifiedDoctrineCase save(UnifiedDoctrineCase doctrineCase) {
        Objects.requireNonNull(doctrineCase);
        UnifiedDoctrineCase existing = data.get(doctrineCase.id());
        if (existing != null && !existing.equals(doctrineCase)) {
            throw new IllegalStateException("seasonal doctrine identity already contains another immutable result");
        }
        data.putIfAbsent(doctrineCase.id(), doctrineCase);
        return data.get(doctrineCase.id());
    }
    @Override public synchronized Optional<UnifiedDoctrineCase> findById(UnifiedDoctrineCaseId id) {
        return Optional.ofNullable(data.get(Objects.requireNonNull(id)));
    }
    @Override public synchronized Optional<UnifiedDoctrineCase> findBySeasonAndKey(SeasonPeriod season, JurisprudentialSimilarityKey key) {
        return findById(UnifiedDoctrineCaseId.from(season, key));
    }
    @Override public synchronized List<UnifiedDoctrineCase> findBySeason(SeasonPeriod season) {
        Objects.requireNonNull(season);
        return data.values().stream().filter(value -> value.seasonPeriod().equals(season)).toList();
    }
    @Override public synchronized List<UnifiedDoctrineCase> findAll() { return List.copyOf(data.values()); }
    @Override public synchronized long count() { return data.size(); }
}
