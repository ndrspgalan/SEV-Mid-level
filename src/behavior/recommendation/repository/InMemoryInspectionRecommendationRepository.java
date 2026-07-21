package behavior.recommendation.repository;

import behavior.recommendation.profile.*;
import java.util.*;

public final class InMemoryInspectionRecommendationRepository implements InspectionRecommendationRepository {
    private final Map<InspectionRecommendationId, InspectionRecommendation> data = new LinkedHashMap<>();

    @Override
    public synchronized void replaceAll(Collection<InspectionRecommendation> recommendations) {
        Objects.requireNonNull(recommendations);
        data.clear();
        for (InspectionRecommendation recommendation : recommendations) {
            if (data.put(recommendation.id(), recommendation) != null) {
                throw new IllegalArgumentException("duplicate recommendation id");
            }
        }
    }

    @Override
    public synchronized Optional<InspectionRecommendation> findById(InspectionRecommendationId id) {
        return Optional.ofNullable(data.get(Objects.requireNonNull(id)));
    }

    @Override
    public synchronized List<InspectionRecommendation> findAll() {
        return List.copyOf(data.values());
    }

    @Override
    public synchronized long count() {
        return data.size();
    }
}
