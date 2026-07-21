package inspection.doctrine.repository;

import behavior.temporal.SeasonPeriod;
import inspection.doctrine.casefile.*;
import inspection.jurisprudence.casefile.JurisprudentialSimilarityKey;
import java.util.*;

/** Append-only repository of seasonal doctrine. */
public interface UnifiedDoctrineCaseRepository {
    UnifiedDoctrineCase save(UnifiedDoctrineCase doctrineCase);
    Optional<UnifiedDoctrineCase> findById(UnifiedDoctrineCaseId id);
    Optional<UnifiedDoctrineCase> findBySeasonAndKey(SeasonPeriod season, JurisprudentialSimilarityKey key);
    List<UnifiedDoctrineCase> findBySeason(SeasonPeriod season);
    List<UnifiedDoctrineCase> findAll();
    long count();
}
