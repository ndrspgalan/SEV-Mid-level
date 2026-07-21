package behavior.temporal.repository;
import behavior.temporal.profile.*;
import java.util.*;
public final class InMemoryProfessionalBehaviorProfileRepository implements ProfessionalBehaviorProfileRepository {
    private final Map<ProfessionalBehaviorProfileId,ProfessionalBehaviorProfile> values=new LinkedHashMap<>();
    public synchronized void replaceAll(Iterable<ProfessionalBehaviorProfile> profiles){ LinkedHashMap<ProfessionalBehaviorProfileId,ProfessionalBehaviorProfile> next=new LinkedHashMap<>(); for(var p:profiles){ if(next.putIfAbsent(p.id(),p)!=null)throw new IllegalArgumentException("duplicate professional profile: "+p.id()); } values.clear();values.putAll(next); }
    public synchronized Optional<ProfessionalBehaviorProfile> findById(ProfessionalBehaviorProfileId id){return Optional.ofNullable(values.get(Objects.requireNonNull(id)));}
    public synchronized List<ProfessionalBehaviorProfile> findAll(){return List.copyOf(values.values());}
    public synchronized long count(){return values.size();}
}
