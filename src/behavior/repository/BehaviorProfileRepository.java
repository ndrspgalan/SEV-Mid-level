package behavior.repository;

import behavior.profile.BehaviorProfile;
import behavior.profile.BehaviorProfileId;
import banking.identity.ConsumerId;

import java.util.List;
import java.util.Optional;

public interface BehaviorProfileRepository {
    void replaceAll(Iterable<BehaviorProfile> profiles);
    Optional<BehaviorProfile> findById(BehaviorProfileId id);
    Optional<BehaviorProfile> findByConsumerId(ConsumerId consumerId);
    List<BehaviorProfile> findAll();
    long count();
}
