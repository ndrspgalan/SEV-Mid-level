package accountHistory;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public record AccountHistoryStatistics(long totalEvents, long completedEvents, long rejectedEvents,
                                       long professionChanges, long holderReleases, long holderAssignments,
                                       Instant firstEventAt, Instant lastEventAt,
                                       Duration averageTimeBetweenProfessionChanges,
                                       Map<AccountHistoryEventType, Long> byType,
                                       Map<AccountHistoryEventStatus, Long> byStatus) {
    public AccountHistoryStatistics {
        byType = Map.copyOf(byType);
        byStatus = Map.copyOf(byStatus);
    }
    public Optional<Instant> firstEvent() { return Optional.ofNullable(firstEventAt); }
    public Optional<Instant> lastEvent() { return Optional.ofNullable(lastEventAt); }
    public Optional<Duration> averageProfessionChangeInterval() { return Optional.ofNullable(averageTimeBetweenProfessionChanges); }
}
