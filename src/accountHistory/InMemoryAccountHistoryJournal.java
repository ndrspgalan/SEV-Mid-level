package accountHistory;

import java.util.*;

public final class InMemoryAccountHistoryJournal implements AccountHistoryJournal {
    private final Map<AccountHistoryEventId, AccountHistoryEvent> events = new LinkedHashMap<>();
    @Override public synchronized void append(AccountHistoryEvent event) {
        Objects.requireNonNull(event);
        if (events.putIfAbsent(event.eventId(), event) != null) throw new IllegalStateException("duplicate account history event id");
    }
    @Override public synchronized Optional<AccountHistoryEvent> findById(AccountHistoryEventId id) { return Optional.ofNullable(events.get(Objects.requireNonNull(id))); }
    @Override public synchronized List<AccountHistoryEvent> findAll() { return List.copyOf(new ArrayList<>(events.values())); }
}
