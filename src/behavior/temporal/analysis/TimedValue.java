package behavior.temporal.analysis;

import java.time.Instant;
import java.util.Objects;

/** One non-negative magnitude observed at a frozen instant. */
public record TimedValue(Instant occurredAt, int value) {
    public TimedValue {
        Objects.requireNonNull(occurredAt);
        if (value < 0) throw new IllegalArgumentException("value must not be negative");
    }
}
