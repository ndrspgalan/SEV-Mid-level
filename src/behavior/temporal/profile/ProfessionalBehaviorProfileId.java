package behavior.temporal.profile;

import banking.identity.ConsumerId;
import banking.identity.ProfessionCode;
import behavior.temporal.SeasonPeriod;
import java.util.Objects;

public record ProfessionalBehaviorProfileId(ConsumerId consumerId, ProfessionCode professionCode, String seasonPeriod) {
    public ProfessionalBehaviorProfileId {
        Objects.requireNonNull(consumerId); Objects.requireNonNull(professionCode);
        if (seasonPeriod == null || seasonPeriod.isBlank()) throw new IllegalArgumentException("season period must not be blank");
    }
    public static ProfessionalBehaviorProfileId of(ConsumerId consumerId, ProfessionCode code, SeasonPeriod season) {
        return new ProfessionalBehaviorProfileId(consumerId, code, season.label());
    }
}
