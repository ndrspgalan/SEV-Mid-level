package inspection.doctrine.refund.repository;

import inspection.doctrine.refund.casefile.*;
import inspection.jurisprudence.casefile.JurisprudentialSimilarityKey;
import java.util.*;

public interface RefundedDoctrineCaseRepository {
    RefundedDoctrineCase save(RefundedDoctrineCase doctrineCase);
    Optional<RefundedDoctrineCase> findById(RefundedDoctrineCaseId id);
    Optional<RefundedDoctrineCase> findBySimilarityKey(JurisprudentialSimilarityKey key);
    List<RefundedDoctrineCase> findAll();
    long count();
}
