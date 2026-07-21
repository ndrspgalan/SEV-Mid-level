package behavior.recommendation.repository;

import behavior.recommendation.profile.*;
import java.util.*;

public interface InspectionRecommendationRepository {
    void replaceAll(Collection<InspectionRecommendation> recommendations);
    Optional<InspectionRecommendation> findById(InspectionRecommendationId id);
    List<InspectionRecommendation> findAll();
    long count();
}
