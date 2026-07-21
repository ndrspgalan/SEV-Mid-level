package application.audit;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record InvariantAuditReport(Instant auditedAt, List<InvariantViolation> violations) {
    public InvariantAuditReport {
        auditedAt = Objects.requireNonNull(auditedAt);
        violations = List.copyOf(Objects.requireNonNull(violations));
    }

    public boolean isValid() { return violations.isEmpty(); }

    public void requireValid() {
        if (!isValid()) {
            throw new IllegalStateException("SEV invariant audit failed: " + violations);
        }
    }
}
