package institutional.repository;
import behavior.temporal.SeasonPeriod;
import institutional.snapshot.SeasonSnapshot;
import java.util.*;
public interface SeasonSnapshotRepository {
 void replaceAll(Iterable<SeasonSnapshot> snapshots);
 Optional<SeasonSnapshot> findByPeriod(SeasonPeriod period);
 List<SeasonSnapshot> findAll();
 long count();
}
