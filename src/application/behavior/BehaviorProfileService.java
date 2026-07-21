package application.behavior;

import banking.identity.ConsumerId;
import behavior.aggregation.BehaviorAggregator;
import behavior.profile.BehaviorProfile;
import behavior.repository.BehaviorProfileRepository;
import economicEvent.repository.EconomicEventRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Application boundary that rebuilds and exposes the M2 behavioral projection. */
public final class BehaviorProfileService {
    private final EconomicEventRepository economicEvents;
    private final BehaviorProfileRepository profiles;
    private final BehaviorAggregator aggregator;

    public BehaviorProfileService(EconomicEventRepository economicEvents,
                                  BehaviorProfileRepository profiles,
                                  BehaviorAggregator aggregator) {
        this.economicEvents = Objects.requireNonNull(economicEvents);
        this.profiles = Objects.requireNonNull(profiles);
        this.aggregator = Objects.requireNonNull(aggregator);
    }

    public List<BehaviorProfile> rebuildProfiles() {
        List<BehaviorProfile> rebuilt = aggregator.aggregate(economicEvents.findAll());
        profiles.replaceAll(rebuilt);
        return rebuilt;
    }

    public Optional<BehaviorProfile> findByConsumerId(ConsumerId consumerId) {
        return profiles.findByConsumerId(Objects.requireNonNull(consumerId));
    }

    public List<BehaviorProfile> findAll() { return profiles.findAll(); }
    public long count() { return profiles.count(); }
}
