package behavior.temporal.repository;
import behavior.temporal.profile.*;
import java.util.*;
public interface ProfessionalBehaviorProfileRepository {
    void replaceAll(Iterable<ProfessionalBehaviorProfile> profiles);
    Optional<ProfessionalBehaviorProfile> findById(ProfessionalBehaviorProfileId id);
    List<ProfessionalBehaviorProfile> findAll();
    long count();
}
