package application.analytics;

import economicEvent.EconomicEvent;
import economicEvent.EconomicEventId;
import economicEvent.query.*;
import economicEvent.repository.EconomicEventRepository;

import java.util.*;

/** Read-side application service over the canonical analytical repository. */
public final class EconomicEventQueryService {
    private final EconomicEventRepository repository;

    public EconomicEventQueryService(EconomicEventRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository must not be null");
    }

    public Optional<EconomicEvent> findById(EconomicEventId id) {
        return repository.findById(Objects.requireNonNull(id, "id must not be null"));
    }

    public EconomicEventPage<EconomicEvent> search(EconomicEventQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        List<EconomicEvent> matching = matchingEvents(query);
        long totalElements = matching.size();
        int totalPages = totalPages(totalElements, query.pageRequest().pageSize());
        int offset = query.pageRequest().offset();
        List<EconomicEvent> content;
        if (offset >= matching.size()) {
            content = List.of();
        } else {
            int endExclusive = Math.min(offset + query.pageRequest().pageSize(), matching.size());
            content = matching.subList(offset, endExclusive);
        }
        return new EconomicEventPage<>(content, query.pageRequest().pageNumber(),
                query.pageRequest().pageSize(), totalElements, totalPages);
    }

    public List<EconomicEvent> matchingEvents(EconomicEventQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        Comparator<EconomicEvent> comparator = Comparator.comparing(EconomicEvent::occurredAt)
                .thenComparing(EconomicEvent::id);
        if (query.sortDirection() == EconomicEventSortDirection.NEWEST_FIRST) comparator = comparator.reversed();
        return repository.find(query).stream().sorted(comparator).toList();
    }

    private int totalPages(long totalElements, int pageSize) {
        return totalElements == 0 ? 0 : Math.toIntExact((totalElements + pageSize - 1) / pageSize);
    }
}
