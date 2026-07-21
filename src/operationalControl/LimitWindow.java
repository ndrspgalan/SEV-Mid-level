package operationalControl;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public enum LimitWindow {
    DAILY, WEEKLY, MONTHLY;

    public Instant start(Instant instant, ZoneId zone) {
        ZonedDateTime value = instant.atZone(zone);
        return switch (this) {
            case DAILY -> value.toLocalDate().atStartOfDay(zone).toInstant();
            case WEEKLY -> value.toLocalDate().minusDays(value.getDayOfWeek().getValue() - 1L).atStartOfDay(zone).toInstant();
            case MONTHLY -> value.toLocalDate().withDayOfMonth(1).atStartOfDay(zone).toInstant();
        };
    }
    public Instant endExclusive(Instant instant, ZoneId zone) {
        Instant start = start(instant, zone);
        ZonedDateTime value = start.atZone(zone);
        return switch (this) {
            case DAILY -> value.plusDays(1).toInstant();
            case WEEKLY -> value.plusWeeks(1).toInstant();
            case MONTHLY -> value.plusMonths(1).toInstant();
        };
    }
}
