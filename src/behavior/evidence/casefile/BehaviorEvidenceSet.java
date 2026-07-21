package behavior.evidence.casefile;

import banking.identity.ConsumerId;
import banking.identity.Profession;
import behavior.deviation.profile.BehaviorDeviationProfileId;
import behavior.expected.profile.ExpectedBehaviorSetId;
import behavior.temporal.SeasonPeriod;
import java.util.*;

/**
 * First explicit analytical case file in SEV.
 *
 * <p>The case file is an immutable, reproducible organization of every M3.2
 * deviation plus its frozen institutional context. Opening a case file is not
 * an alert and does not imply that any evidence is relevant or suspicious.</p>
 */
public record BehaviorEvidenceSet(
        BehaviorEvidenceSetId id,
        ConsumerId consumerId,
        Profession profession,
        SeasonPeriod seasonPeriod,
        BehaviorDeviationProfileId sourceDeviationProfileId,
        ExpectedBehaviorSetId expectedBehaviorSetId,
        InstitutionalContext institutionalContext,
        Map<BehaviorEvidenceCategory,List<BehaviorEvidence>> evidenceByCategory
) {
    public BehaviorEvidenceSet {
        Objects.requireNonNull(id);Objects.requireNonNull(consumerId);Objects.requireNonNull(profession);Objects.requireNonNull(seasonPeriod);
        Objects.requireNonNull(sourceDeviationProfileId);Objects.requireNonNull(expectedBehaviorSetId);Objects.requireNonNull(institutionalContext);
        if(!id.equals(BehaviorEvidenceSetId.of(consumerId,profession.code(),seasonPeriod))) throw new IllegalArgumentException("case file identity mismatch");
        if(!sourceDeviationProfileId.consumerId().equals(consumerId)||!sourceDeviationProfileId.professionCode().equals(profession.code())||!sourceDeviationProfileId.seasonPeriod().equals(seasonPeriod.label())) throw new IllegalArgumentException("source deviation profile mismatch");
        if(!expectedBehaviorSetId.professionCode().equals(profession.code())||!expectedBehaviorSetId.seasonPeriod().equals(seasonPeriod.label())) throw new IllegalArgumentException("expected behavior reference mismatch");
        if(!institutionalContext.profession().equals(profession)||!institutionalContext.seasonPeriod().equals(seasonPeriod)) throw new IllegalArgumentException("institutional context mismatch");
        EnumMap<BehaviorEvidenceCategory,List<BehaviorEvidence>> copy=new EnumMap<>(BehaviorEvidenceCategory.class);
        Objects.requireNonNull(evidenceByCategory).forEach((category,items)->{
            Objects.requireNonNull(category);List<BehaviorEvidence> frozen=List.copyOf(Objects.requireNonNull(items));
            for(BehaviorEvidence evidence:frozen) if(evidence.category()!=category) throw new IllegalArgumentException("evidence category mismatch");
            copy.put(category,frozen);
        });
        for(BehaviorEvidenceCategory category:BehaviorEvidenceCategory.values()) copy.putIfAbsent(category,List.of());
        evidenceByCategory=Collections.unmodifiableMap(copy);
    }
    public int evidenceCount(){return evidenceByCategory.values().stream().mapToInt(List::size).sum();}
    public List<BehaviorEvidence> allEvidence(){return evidenceByCategory.values().stream().flatMap(Collection::stream).toList();}
}
