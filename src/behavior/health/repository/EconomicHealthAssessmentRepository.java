package behavior.health.repository;

import behavior.health.profile.*;
import java.util.*;

public interface EconomicHealthAssessmentRepository {
    void replaceAll(Collection<EconomicHealthAssessment> assessments);
    Optional<EconomicHealthAssessment> findById(EconomicHealthAssessmentId id);
    List<EconomicHealthAssessment> findAll();
    long count();
}
