package behavior.temporal.profile;

import behavior.temporal.DayPeriod;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Total activity observed during one complete Valerian season.
 *
 * <p>This is deliberately not a {@link WindowStatistics}: a season supplies one
 * total, so median, mode and deviation would be formally calculable but
 * analytically empty.</p>
 */
public record SeasonActivitySummary(
        long total,
        Instant firstObservedAt,
        Instant lastObservedAt,
        Map<DayPeriod, Long> byDayPeriod
) {
    public SeasonActivitySummary {
        if (total <= 0) throw new IllegalArgumentException("season total must be positive");
        Objects.requireNonNull(firstObservedAt);
        Objects.requireNonNull(lastObservedAt);
        if (lastObservedAt.isBefore(firstObservedAt)) throw new IllegalArgumentException("invalid seasonal interval");
        Objects.requireNonNull(byDayPeriod);
        EnumMap<DayPeriod, Long> copy = new EnumMap<>(DayPeriod.class);
        long classified = 0;
        for (DayPeriod period : DayPeriod.values()) {
            long value = byDayPeriod.getOrDefault(period, 0L);
            if (value < 0) throw new IllegalArgumentException("negative day-period total");
            copy.put(period, value);
            classified += value;
        }
        if (classified != total) throw new IllegalArgumentException("day-period totals do not match season total");
        byDayPeriod = Collections.unmodifiableMap(copy);
    }
}
