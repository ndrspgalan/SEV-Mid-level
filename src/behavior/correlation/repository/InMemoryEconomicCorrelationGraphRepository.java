package behavior.correlation.repository;

import behavior.correlation.profile.*;
import java.util.*;

public final class InMemoryEconomicCorrelationGraphRepository implements EconomicCorrelationGraphRepository {
    private final Map<EconomicCorrelationGraphId, EconomicCorrelationGraph> data = new LinkedHashMap<>();

    @Override
    public synchronized void replaceAll(Collection<EconomicCorrelationGraph> graphs) {
        Objects.requireNonNull(graphs);
        data.clear();
        for (EconomicCorrelationGraph graph : graphs) {
            if (data.put(graph.id(), graph) != null) throw new IllegalArgumentException("duplicate graph id");
        }
    }

    @Override public synchronized Optional<EconomicCorrelationGraph> findById(EconomicCorrelationGraphId id) {
        return Optional.ofNullable(data.get(Objects.requireNonNull(id)));
    }
    @Override public synchronized List<EconomicCorrelationGraph> findAll() { return List.copyOf(data.values()); }
    @Override public synchronized long count() { return data.size(); }
}
