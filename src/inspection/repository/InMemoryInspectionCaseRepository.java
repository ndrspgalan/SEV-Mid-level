package inspection.repository;

import inspection.casefile.*;
import java.util.*;

public final class InMemoryInspectionCaseRepository implements InspectionCaseRepository {
    private final Map<InspectionCaseId, InspectionCase> data = new LinkedHashMap<>();

    @Override
    public synchronized InspectionCase save(InspectionCase inspectionCase) {
        Objects.requireNonNull(inspectionCase);
        InspectionCase existing = data.get(inspectionCase.id());
        if (existing != null && !existing.sourceRecommendation().equals(inspectionCase.sourceRecommendation())) {
            throw new IllegalStateException("inspection case identity already belongs to another recommendation snapshot");
        }
        if (existing != null && existing.status() == InspectionCaseStatus.CLOSED
                && inspectionCase.status() == InspectionCaseStatus.OPEN) {
            throw new IllegalStateException("a closed inspection case cannot be reopened");
        }
        data.put(inspectionCase.id(), inspectionCase);
        return inspectionCase;
    }

    @Override
    public synchronized Optional<InspectionCase> findById(InspectionCaseId id) {
        return Optional.ofNullable(data.get(Objects.requireNonNull(id)));
    }

    @Override
    public synchronized List<InspectionCase> findAll() {
        return List.copyOf(data.values());
    }

    @Override
    public synchronized long count() {
        return data.size();
    }
}
