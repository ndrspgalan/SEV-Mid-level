package operationalControl;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
public record OperationalAuthorization(UUID token, OperationalControlSnapshot snapshot) {
 public OperationalAuthorization { Objects.requireNonNull(token); Objects.requireNonNull(snapshot); }
 public boolean allowed(){return snapshot.allowed();} public Optional<OperationalControlRejectionReason> rejectionReason(){return snapshot.rejectionReason();}
}
