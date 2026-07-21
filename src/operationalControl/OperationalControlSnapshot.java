package operationalControl;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
public record OperationalControlSnapshot(Instant decidedAt, boolean allowed, List<OperationalPolicyId> appliedPolicyIds,
 long usageAmountBefore, int usageCountBefore, long projectedAmount, int projectedCount,
 Optional<OperationalControlRejectionReason> rejectionReason) {
 public OperationalControlSnapshot { Objects.requireNonNull(decidedAt); appliedPolicyIds=List.copyOf(appliedPolicyIds); Objects.requireNonNull(rejectionReason); }
}
