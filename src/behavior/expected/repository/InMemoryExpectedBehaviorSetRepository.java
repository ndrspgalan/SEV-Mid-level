package behavior.expected.repository;
import behavior.expected.profile.*;
import java.util.*;
public final class InMemoryExpectedBehaviorSetRepository implements ExpectedBehaviorSetRepository {
    private final Map<ExpectedBehaviorSetId,ExpectedBehaviorSet> data=new LinkedHashMap<>();
    @Override public synchronized void replaceAll(Iterable<ExpectedBehaviorSet> sets){data.clear();for(ExpectedBehaviorSet set:sets){Objects.requireNonNull(set);if(data.putIfAbsent(set.id(),set)!=null)throw new IllegalArgumentException("duplicate expected behavior set: "+set.id());}}
    @Override public synchronized Optional<ExpectedBehaviorSet> findById(ExpectedBehaviorSetId id){return Optional.ofNullable(data.get(Objects.requireNonNull(id)));}
    @Override public synchronized List<ExpectedBehaviorSet> findAll(){return List.copyOf(data.values());}
    @Override public synchronized long count(){return data.size();}
}
