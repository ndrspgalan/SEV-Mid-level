package inspection.resolution.casefile;

import java.util.Objects;

/**
 * Restrictive institutional measure attached exclusively to FRAUD.
 *
 * It is not a declaration of moral or legal guilt and it is not punishment in
 * SEV's domain. It constrains an economically unjustifiable operating margin so
 * the deployment can recover a behavior that SEV is able to explain.
 */
public record RestrictiveMeasure(
        InstitutionalActionScope scope,
        String target,
        String description
) {
    public RestrictiveMeasure {
        Objects.requireNonNull(scope, "restrictive measure scope must not be null");
        target = requireText(target, "restrictive measure target");
        description = requireText(description, "restrictive measure description");
    }

    private static String requireText(String value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return normalized;
    }
}
