package operationalControl;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
public final class OperationalPolicyRegistry {
 private final Map<OperationalPolicyId,OperationalLimitPolicy> policies=new LinkedHashMap<>();
 public synchronized void register(OperationalLimitPolicy policy){ Objects.requireNonNull(policy); if(policies.putIfAbsent(policy.id(),policy)!=null) throw new IllegalArgumentException("policy already registered"); }
 public synchronized void deactivate(OperationalPolicyId id,Instant at){ require(id).deactivateAt(at); }
 public synchronized OperationalLimitPolicy require(OperationalPolicyId id){ OperationalLimitPolicy p=policies.get(Objects.requireNonNull(id)); if(p==null) throw new IllegalArgumentException("policy not found: "+id); return p; }
 public synchronized List<OperationalLimitPolicy> all(){return List.copyOf(policies.values());}
 public synchronized List<OperationalLimitPolicy> effectiveAt(Instant at){return policies.values().stream().filter(p->p.isEffectiveAt(at)).toList();}
}
