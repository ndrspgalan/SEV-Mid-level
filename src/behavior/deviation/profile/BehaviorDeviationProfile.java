package behavior.deviation.profile;

import banking.identity.ConsumerId;
import banking.identity.Profession;
import behavior.expected.profile.ExpectedBehaviorMetric;
import behavior.expected.profile.ExpectedBehaviorSetId;
import behavior.temporal.SeasonPeriod;
import java.util.*;

/** Complete descriptive comparison of one holder against one population reference. */
public record BehaviorDeviationProfile(
        BehaviorDeviationProfileId id,
        ConsumerId consumerId,
        Profession profession,
        SeasonPeriod seasonPeriod,
        ExpectedBehaviorSetId expectedBehaviorSetId,
        Map<ExpectedBehaviorMetric, BehaviorDeviation> deviations
) {
    public BehaviorDeviationProfile {
        Objects.requireNonNull(id); Objects.requireNonNull(consumerId); Objects.requireNonNull(profession);
        Objects.requireNonNull(seasonPeriod); Objects.requireNonNull(expectedBehaviorSetId);
        if (!id.equals(BehaviorDeviationProfileId.of(consumerId, profession.code(), seasonPeriod)))
            throw new IllegalArgumentException("deviation profile identity mismatch");
        if (!expectedBehaviorSetId.equals(ExpectedBehaviorSetId.of(profession.code(), seasonPeriod)))
            throw new IllegalArgumentException("expected behavior reference mismatch");
        TreeMap<ExpectedBehaviorMetric, BehaviorDeviation> copy = new TreeMap<>();
        Objects.requireNonNull(deviations).forEach((k,v) -> copy.put(Objects.requireNonNull(k), Objects.requireNonNull(v)));
        deviations = Collections.unmodifiableMap(copy);
    }
}
