package economicEvent.repository;

import economicEvent.EconomicEvent;
import economicEvent.EconomicEventId;
import economicEvent.query.EconomicEventQuery;

import java.util.List;
import java.util.Optional;

/** Storage boundary for canonical economic events. */
public interface EconomicEventRepository {
    EconomicEventSaveResult save(EconomicEvent event);
    EconomicEventBatchSaveResult saveAll(Iterable<EconomicEvent> events);
    Optional<EconomicEvent> findById(EconomicEventId id);
    List<EconomicEvent> findAll();
    List<EconomicEvent> find(EconomicEventQuery query);
    boolean exists(EconomicEventId id);
    long count();
}
