package behavior.expected.repository;
import behavior.expected.profile.*;
import java.util.*;
public interface ExpectedBehaviorSetRepository {
    void replaceAll(Iterable<ExpectedBehaviorSet> sets);
    Optional<ExpectedBehaviorSet> findById(ExpectedBehaviorSetId id);
    List<ExpectedBehaviorSet> findAll();
    long count();
}
