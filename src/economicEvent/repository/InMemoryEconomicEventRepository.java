package economicEvent.repository;

import economicEvent.EconomicEvent;
import economicEvent.EconomicEventId;
import economicEvent.query.EconomicEventMatcher;
import economicEvent.query.EconomicEventQuery;

import java.util.*;

/** Thread-safe in-memory analytical repository with deterministic idempotency. */
public final class InMemoryEconomicEventRepository implements EconomicEventRepository {
    private final Map<EconomicEventId, EconomicEvent> events = new LinkedHashMap<>();

    @Override
    public synchronized EconomicEventSaveResult save(EconomicEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        EconomicEvent existing = events.get(event.id());
        if (existing == null) {
            events.put(event.id(), event);
            return EconomicEventSaveResult.CREATED;
        }
        if (existing.equals(event)) return EconomicEventSaveResult.ALREADY_PRESENT;
        throw new IllegalStateException("economic event id collision with different content: " + event.id());
    }

    @Override
    public synchronized EconomicEventBatchSaveResult saveAll(Iterable<EconomicEvent> values) {
        Objects.requireNonNull(values, "events must not be null");
        List<EconomicEvent> batch = new ArrayList<>();
        Map<EconomicEventId, EconomicEvent> staged = new LinkedHashMap<>();
        int created = 0;
        int alreadyPresent = 0;

        for (EconomicEvent event : values) {
            EconomicEvent required = Objects.requireNonNull(event, "event must not be null");
            EconomicEvent stagedExisting = staged.get(required.id());
            if (stagedExisting != null) {
                if (!stagedExisting.equals(required)) {
                    throw new IllegalStateException("economic event id collision inside batch: " + required.id());
                }
                alreadyPresent++;
                batch.add(required);
                continue;
            }

            EconomicEvent repositoryExisting = events.get(required.id());
            if (repositoryExisting == null) {
                created++;
                staged.put(required.id(), required);
            } else if (repositoryExisting.equals(required)) {
                alreadyPresent++;
                staged.put(required.id(), required);
            } else {
                throw new IllegalStateException("economic event id collision with different content: " + required.id());
            }
            batch.add(required);
        }

        for (EconomicEvent event : staged.values()) events.putIfAbsent(event.id(), event);
        return new EconomicEventBatchSaveResult(batch.size(), created, alreadyPresent);
    }

    @Override public synchronized Optional<EconomicEvent> findById(EconomicEventId id) {
        return Optional.ofNullable(events.get(Objects.requireNonNull(id, "id must not be null")));
    }
    @Override public synchronized List<EconomicEvent> findAll() { return List.copyOf(events.values()); }
    @Override public synchronized List<EconomicEvent> find(EconomicEventQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return events.values().stream().filter(event -> EconomicEventMatcher.matches(event, query)).toList();
    }
    @Override public synchronized boolean exists(EconomicEventId id) {
        return events.containsKey(Objects.requireNonNull(id, "id must not be null"));
    }
    @Override public synchronized long count() { return events.size(); }
}
