package inspection.resolution.repository;

import inspection.casefile.InspectionCaseId;
import inspection.resolution.casefile.*;
import java.util.*;

public final class InMemoryInspectionResolutionRepository implements InspectionResolutionRepository {
    private final Map<InspectionResolutionId, InspectionResolution> data = new LinkedHashMap<>();

    @Override
    public synchronized InspectionResolution save(InspectionResolution resolution) {
        Objects.requireNonNull(resolution);
        InspectionResolution existing = data.get(resolution.id());
        if (existing != null) {
            if (existing != resolution) {
                throw new IllegalStateException("inspection case already has a final resolution");
            }
            return existing;
        }
        data.put(resolution.id(), resolution);
        return resolution;
    }

    @Override
    public synchronized Optional<InspectionResolution> findById(InspectionResolutionId id) {
        return Optional.ofNullable(data.get(Objects.requireNonNull(id)));
    }

    @Override
    public synchronized Optional<InspectionResolution> findByCaseId(InspectionCaseId caseId) {
        return findById(InspectionResolutionId.from(caseId));
    }

    @Override
    public synchronized List<InspectionResolution> findAll() { return List.copyOf(data.values()); }

    @Override
    public synchronized long count() { return data.size(); }
}
