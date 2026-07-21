package inspection.resolution.casefile;

import java.util.Objects;

/**
 * A deployment-level change recommended by SEV.
 *
 * The agnostic SEV model is immutable. This value does not mutate its policies;
 * it describes what the society operating one concrete deployment should
 * change in that deployment after receiving SEV's technical diagnosis.
 */
public record PolicyAdjustment(
        InstitutionalActionScope scope,
        PolicyAdjustmentDirection direction,
        String target,
        String description
) {
    public PolicyAdjustment {
        Objects.requireNonNull(scope, "policy adjustment scope must not be null");
        Objects.requireNonNull(direction, "policy adjustment direction must not be null");
        target = requireText(target, "policy adjustment target");
        description = requireText(description, "policy adjustment description");
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return normalized;
    }
}
