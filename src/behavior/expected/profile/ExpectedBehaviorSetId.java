package behavior.expected.profile;

import banking.identity.ProfessionCode;
import behavior.temporal.SeasonPeriod;
import java.util.Objects;

/** Deterministic projection identity: exactly one set per profession and season. */
public record ExpectedBehaviorSetId(ProfessionCode professionCode, String seasonPeriod) {
    public ExpectedBehaviorSetId { Objects.requireNonNull(professionCode); seasonPeriod=Objects.requireNonNull(seasonPeriod).trim(); if(seasonPeriod.isEmpty())throw new IllegalArgumentException("season period required"); }
    public static ExpectedBehaviorSetId of(ProfessionCode code, SeasonPeriod period){return new ExpectedBehaviorSetId(code,period.label());}
}
