package application.history;

import accountHistory.*;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Objects;

public final class AccountHistoryStatisticsService {
    private final AccountHistoryQueryService queryService;
    public AccountHistoryStatisticsService(AccountHistoryQueryService queryService) { this.queryService = Objects.requireNonNull(queryService); }

    public AccountHistoryStatistics calculate(AccountHistoryQuery query) {
        List<AccountHistoryEvent> events = new ArrayList<>();
        int pageNumber = 0;
        while (true) {
            AccountHistoryPage page = queryService.search(query, new AccountHistoryPageRequest(pageNumber, 100));
            events.addAll(page.content());
            if (!page.hasNext()) break;
            pageNumber++;
        }
        EnumMap<AccountHistoryEventType, Long> byType = new EnumMap<>(AccountHistoryEventType.class);
        EnumMap<AccountHistoryEventStatus, Long> byStatus = new EnumMap<>(AccountHistoryEventStatus.class);
        for (AccountHistoryEventType type : AccountHistoryEventType.values()) byType.put(type, 0L);
        for (AccountHistoryEventStatus status : AccountHistoryEventStatus.values()) byStatus.put(status, 0L);
        events.forEach(event -> {
            byType.put(event.type(), byType.get(event.type()) + 1);
            byStatus.put(event.status(), byStatus.get(event.status()) + 1);
        });
        List<Instant> completedProfessionChanges = events.stream()
                .filter(e -> e.type() == AccountHistoryEventType.PROFESSION_CHANGED)
                .filter(e -> e.status() == AccountHistoryEventStatus.COMPLETED)
                .map(AccountHistoryEvent::occurredAt).sorted().toList();
        Duration average = averageInterval(completedProfessionChanges);
        Instant first = events.stream().map(AccountHistoryEvent::occurredAt).min(Instant::compareTo).orElse(null);
        Instant last = events.stream().map(AccountHistoryEvent::occurredAt).max(Instant::compareTo).orElse(null);
        return new AccountHistoryStatistics(events.size(), byStatus.get(AccountHistoryEventStatus.COMPLETED),
                byStatus.get(AccountHistoryEventStatus.REJECTED), byType.get(AccountHistoryEventType.PROFESSION_CHANGED),
                byType.get(AccountHistoryEventType.HOLDER_RELEASED), byType.get(AccountHistoryEventType.HOLDER_ASSIGNED),
                first, last, average, byType, byStatus);
    }

    private Duration averageInterval(List<Instant> instants) {
        if (instants.size() < 2) return null;
        long totalMillis = 0;
        for (int i = 1; i < instants.size(); i++) totalMillis = Math.addExact(totalMillis,
                Duration.between(instants.get(i - 1), instants.get(i)).toMillis());
        return Duration.ofMillis(totalMillis / (instants.size() - 1));
    }
}
