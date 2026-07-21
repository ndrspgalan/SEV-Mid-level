package behavior.correlation.repository;

import behavior.correlation.profile.*;
import java.util.*;

public interface EconomicCorrelationGraphRepository {
    void replaceAll(Collection<EconomicCorrelationGraph> graphs);
    Optional<EconomicCorrelationGraph> findById(EconomicCorrelationGraphId id);
    List<EconomicCorrelationGraph> findAll();
    long count();
}
