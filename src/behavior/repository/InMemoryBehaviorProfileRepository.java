package behavior.repository;

import banking.identity.ConsumerId;
import behavior.profile.BehaviorProfile;
import behavior.profile.BehaviorProfileId;

import java.util.*;

/** Atomic in-memory replacement repository for reproducible behavioral projections. */
public final class InMemoryBehaviorProfileRepository implements BehaviorProfileRepository {
    private final Map<BehaviorProfileId, BehaviorProfile> profiles = new LinkedHashMap<>();

    @Override
    public synchronized void replaceAll(Iterable<BehaviorProfile> values) {
        Objects.requireNonNull(values, "profiles must not be null");
        LinkedHashMap<BehaviorProfileId, BehaviorProfile> replacement = new LinkedHashMap<>();
        for (BehaviorProfile value : values) {
            Objects.requireNonNull(value, "profile must not be null");
            if (replacement.putIfAbsent(value.id(), value) != null) {
                throw new IllegalArgumentException("duplicate behavior profile id: " + value.id());
            }
        }
        profiles.clear();
        profiles.putAll(replacement);
    }

    @Override public synchronized Optional<BehaviorProfile> findById(BehaviorProfileId id) { return Optional.ofNullable(profiles.get(Objects.requireNonNull(id))); }
    @Override public synchronized Optional<BehaviorProfile> findByConsumerId(ConsumerId id) { return findById(new BehaviorProfileId(Objects.requireNonNull(id))); }
    @Override public synchronized List<BehaviorProfile> findAll() { return List.copyOf(profiles.values()); }
    @Override public synchronized long count() { return profiles.size(); }
}
