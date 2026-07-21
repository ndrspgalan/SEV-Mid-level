package institutional.repository;
import behavior.temporal.SeasonPeriod;
import institutional.snapshot.SeasonSnapshot;
import java.util.*;
public final class InMemorySeasonSnapshotRepository implements SeasonSnapshotRepository {
 private final Map<String,SeasonSnapshot> data=new LinkedHashMap<>();
 public synchronized void replaceAll(Iterable<SeasonSnapshot> snapshots){LinkedHashMap<String,SeasonSnapshot> next=new LinkedHashMap<>();for(SeasonSnapshot s:snapshots){if(next.putIfAbsent(s.seasonPeriod().label(),s)!=null)throw new IllegalArgumentException("duplicate season snapshot");}data.clear();data.putAll(next);}
 public synchronized Optional<SeasonSnapshot> findByPeriod(SeasonPeriod p){return Optional.ofNullable(data.get(p.label()));}
 public synchronized List<SeasonSnapshot> findAll(){return List.copyOf(data.values());}
 public synchronized long count(){return data.size();}
}
