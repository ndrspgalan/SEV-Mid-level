package inspection.resolution.casefile;

import java.util.*;

/**
 * Immutable set of deployment actions recommended by an inspection resolution.
 *
 * AUDIT and FRAUD are two stabilization responses, not moral opposites. AUDIT
 * adapts the deployment without targeting an alleged responsible actor. FRAUD
 * also constrains the economic scope whose behavior cannot be justified. One
 * plan may relax some policies while restricting or reconfiguring others.
 */
public final class InstitutionalActionPlan {
    private final List<PolicyAdjustment> policyAdjustments;
    private final List<RestrictiveMeasure> restrictiveMeasures;

    public InstitutionalActionPlan(
            Collection<PolicyAdjustment> policyAdjustments,
            Collection<RestrictiveMeasure> restrictiveMeasures
    ) {
        this.policyAdjustments = immutableNonNullList(policyAdjustments, "policy adjustments");
        this.restrictiveMeasures = immutableNonNullList(restrictiveMeasures, "restrictive measures");
    }

    public static InstitutionalActionPlan empty() {
        return new InstitutionalActionPlan(List.of(), List.of());
    }

    private static <T> List<T> immutableNonNullList(Collection<T> values, String label) {
        Objects.requireNonNull(values, label + " must not be null");
        ArrayList<T> copy = new ArrayList<>(values.size());
        for (T value : values) copy.add(Objects.requireNonNull(value, label + " must not contain null"));
        return List.copyOf(copy);
    }

    public List<PolicyAdjustment> policyAdjustments() { return policyAdjustments; }
    public List<RestrictiveMeasure> restrictiveMeasures() { return restrictiveMeasures; }
    public boolean isEmpty() { return policyAdjustments.isEmpty() && restrictiveMeasures.isEmpty(); }
}
