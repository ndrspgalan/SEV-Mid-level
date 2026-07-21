package behavior.correlation.profile;

import behavior.alignment.profile.StructuralAlignmentId;
import java.util.Objects;

/** One explainable compatible-profile displacement emitted from an M3.4 alignment. */
public record AlignmentDisplacement(
        StructuralAlignmentId alignmentId,
        String declaredProfession,
        String compatibleProfession,
        InstitutionalProfileRelation institutionalRelation,
        AlignmentDisplacementDirection direction
) {
    public AlignmentDisplacement {
        Objects.requireNonNull(alignmentId);
        if (declaredProfession == null || declaredProfession.isBlank()) throw new IllegalArgumentException("declaredProfession");
        if (compatibleProfession == null || compatibleProfession.isBlank()) throw new IllegalArgumentException("compatibleProfession");
        Objects.requireNonNull(institutionalRelation);
        Objects.requireNonNull(direction);
    }

    public String bridgeKey() {
        if (declaredProfession.compareTo(compatibleProfession) <= 0) {
            return declaredProfession + "<->" + compatibleProfession;
        }
        return compatibleProfession + "<->" + declaredProfession;
    }
}
