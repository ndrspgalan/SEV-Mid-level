package behavior.alignment.profile;
import behavior.evidence.casefile.BehaviorEvidenceSetId;
import java.util.Objects;
public record StructuralAlignmentId(String value){public StructuralAlignmentId{if(value==null||value.isBlank())throw new IllegalArgumentException("value");}public static StructuralAlignmentId from(BehaviorEvidenceSetId id){return new StructuralAlignmentId("ALIGN|"+Objects.requireNonNull(id).consumerId().value()+"|"+id.professionCode().value()+"|"+id.seasonPeriod());}}
