package behavior.deviation.profile;

import banking.identity.ConsumerId;
import banking.identity.ProfessionCode;
import behavior.temporal.SeasonPeriod;
import java.util.Objects;

public record BehaviorDeviationProfileId(ConsumerId consumerId, ProfessionCode professionCode, String seasonPeriod) {
    public BehaviorDeviationProfileId {
        Objects.requireNonNull(consumerId); Objects.requireNonNull(professionCode);
        if (seasonPeriod == null || seasonPeriod.isBlank()) throw new IllegalArgumentException("season period must not be blank");
    }
    public static BehaviorDeviationProfileId of(ConsumerId consumerId, ProfessionCode professionCode, SeasonPeriod seasonPeriod) {
        return new BehaviorDeviationProfileId(consumerId, professionCode, seasonPeriod.label());
    }
}
