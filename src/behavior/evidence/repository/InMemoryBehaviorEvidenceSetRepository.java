package behavior.evidence.repository;
import behavior.evidence.casefile.*;import java.util.*;
public final class InMemoryBehaviorEvidenceSetRepository implements BehaviorEvidenceSetRepository{
 private final Map<BehaviorEvidenceSetId,BehaviorEvidenceSet> data=new LinkedHashMap<>();
 public synchronized void replaceAll(Collection<BehaviorEvidenceSet> sets){data.clear();for(BehaviorEvidenceSet set:sets){if(data.put(set.id(),set)!=null)throw new IllegalArgumentException("duplicate case file id");}}
 public synchronized Optional<BehaviorEvidenceSet> findById(BehaviorEvidenceSetId id){return Optional.ofNullable(data.get(Objects.requireNonNull(id)));}
 public synchronized List<BehaviorEvidenceSet> findAll(){return List.copyOf(data.values());}
 public synchronized long count(){return data.size();}
}
