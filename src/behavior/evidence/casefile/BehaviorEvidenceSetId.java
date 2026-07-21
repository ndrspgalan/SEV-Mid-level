package behavior.evidence.casefile;

import banking.identity.ConsumerId;
import banking.identity.ProfessionCode;
import behavior.temporal.SeasonPeriod;
import java.util.Objects;

/** Deterministic identity of one analytical case file. */
public record BehaviorEvidenceSetId(ConsumerId consumerId, ProfessionCode professionCode, String seasonPeriod) {
    public BehaviorEvidenceSetId {
        Objects.requireNonNull(consumerId); Objects.requireNonNull(professionCode);
        if(seasonPeriod==null||seasonPeriod.isBlank()) throw new IllegalArgumentException("season period must not be blank");
    }
    public static BehaviorEvidenceSetId of(ConsumerId consumerId, ProfessionCode professionCode, SeasonPeriod seasonPeriod){
        return new BehaviorEvidenceSetId(consumerId,professionCode,seasonPeriod.label());
    }
}
