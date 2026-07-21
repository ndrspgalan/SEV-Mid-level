package behavior.health.repository;

import behavior.health.profile.*;
import java.util.*;

public final class InMemoryEconomicHealthAssessmentRepository implements EconomicHealthAssessmentRepository {
    private final Map<EconomicHealthAssessmentId, EconomicHealthAssessment> data = new LinkedHashMap<>();
    @Override public synchronized void replaceAll(Collection<EconomicHealthAssessment> assessments) {
        Objects.requireNonNull(assessments); data.clear();
        for (EconomicHealthAssessment assessment : assessments) if (data.put(assessment.id(), assessment) != null) throw new IllegalArgumentException("duplicate assessment id");
    }
    @Override public synchronized Optional<EconomicHealthAssessment> findById(EconomicHealthAssessmentId id) { return Optional.ofNullable(data.get(Objects.requireNonNull(id))); }
    @Override public synchronized List<EconomicHealthAssessment> findAll() { return List.copyOf(data.values()); }
    @Override public synchronized long count() { return data.size(); }
}
