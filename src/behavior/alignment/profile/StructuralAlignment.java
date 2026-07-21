package behavior.alignment.profile;
import banking.identity.*;import behavior.evidence.casefile.BehaviorEvidenceSetId;import behavior.temporal.SeasonPeriod;import java.util.*;
/** M3.4 institutional compatibility map. It deliberately emits no health or inspection conclusion. */
public record StructuralAlignment(StructuralAlignmentId id,BehaviorEvidenceSetId sourceCaseFileId,ConsumerId consumerId,Profession declaredProfession,SeasonPeriod seasonPeriod,Map<String,ProfileCompatibility> comparisons){
 public StructuralAlignment{Objects.requireNonNull(id);Objects.requireNonNull(sourceCaseFileId);Objects.requireNonNull(consumerId);Objects.requireNonNull(declaredProfession);Objects.requireNonNull(seasonPeriod);if(!id.equals(StructuralAlignmentId.from(sourceCaseFileId)))throw new IllegalArgumentException("alignment identity mismatch");comparisons=Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(comparisons)));}
 public List<ProfileCompatibility> compatibleProfiles(){return comparisons.values().stream().filter(ProfileCompatibility::compatible).toList();}
 public List<ProfileCompatibility> crossProfiles(){return comparisons.values().stream().filter(p->p.crossProfile(declaredProfession.name())).toList();}
 public Optional<ProfileCompatibility> declaredProfile(){return Optional.ofNullable(comparisons.get(declaredProfession.name()));}
}
