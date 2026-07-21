package behavior.deviation.repository;

import behavior.deviation.profile.*;
import java.util.*;

public final class InMemoryBehaviorDeviationProfileRepository implements BehaviorDeviationProfileRepository {
    private final Map<BehaviorDeviationProfileId,BehaviorDeviationProfile> data=new LinkedHashMap<>();
    @Override public synchronized void replaceAll(Collection<BehaviorDeviationProfile> profiles){data.clear();for(BehaviorDeviationProfile p:profiles)data.put(p.id(),p);}
    @Override public synchronized Optional<BehaviorDeviationProfile> findById(BehaviorDeviationProfileId id){return Optional.ofNullable(data.get(Objects.requireNonNull(id)));}
    @Override public synchronized List<BehaviorDeviationProfile> findAll(){return List.copyOf(data.values());}
    @Override public synchronized long count(){return data.size();}
}
