package behavior.evidence.repository;
import behavior.evidence.casefile.*;import java.util.*;
public interface BehaviorEvidenceSetRepository{void replaceAll(Collection<BehaviorEvidenceSet> sets);Optional<BehaviorEvidenceSet> findById(BehaviorEvidenceSetId id);List<BehaviorEvidenceSet> findAll();long count();}
