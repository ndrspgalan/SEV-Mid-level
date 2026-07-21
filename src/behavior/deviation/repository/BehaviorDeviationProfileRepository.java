package behavior.deviation.repository;

import behavior.deviation.profile.*;
import java.util.*;

public interface BehaviorDeviationProfileRepository {
    void replaceAll(Collection<BehaviorDeviationProfile> profiles);
    Optional<BehaviorDeviationProfile> findById(BehaviorDeviationProfileId id);
    List<BehaviorDeviationProfile> findAll();
    long count();
}
