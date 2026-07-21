package application.analytics.projection;

import java.util.List;
import java.util.Objects;

public record EconomicEventProjectionResult(
        int inspected,
        int created,
        int alreadyPresent,
        List<EconomicEventProjectionFailure> failures) {
    public EconomicEventProjectionResult {
        if (inspected < 0 || created < 0 || alreadyPresent < 0) throw new IllegalArgumentException("projection counters must not be negative");
        failures = List.copyOf(Objects.requireNonNull(failures));
        if (created + alreadyPresent + failures.size() > inspected) {
            throw new IllegalArgumentException("projection counters exceed inspected sources");
        }
    }
    public int failed() { return failures.size(); }
    public boolean successful() { return failures.isEmpty(); }

    public EconomicEventProjectionResult plus(EconomicEventProjectionResult other) {
        Objects.requireNonNull(other);
        java.util.ArrayList<EconomicEventProjectionFailure> merged = new java.util.ArrayList<>(failures);
        merged.addAll(other.failures);
        return new EconomicEventProjectionResult(inspected + other.inspected, created + other.created,
                alreadyPresent + other.alreadyPresent, merged);
    }

    public static EconomicEventProjectionResult empty() {
        return new EconomicEventProjectionResult(0, 0, 0, List.of());
    }
}
