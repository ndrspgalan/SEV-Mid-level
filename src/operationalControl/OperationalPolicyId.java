package operationalControl;
import java.util.Objects;
import java.util.UUID;
public record OperationalPolicyId(UUID value) {
 public OperationalPolicyId { Objects.requireNonNull(value); }
 public static OperationalPolicyId generate(){ return new OperationalPolicyId(UUID.randomUUID()); }
 @Override public String toString(){ return value.toString(); }
}
