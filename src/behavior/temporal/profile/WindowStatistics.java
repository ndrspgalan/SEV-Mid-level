package behavior.temporal.profile;

import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;

/** Descriptive statistics over occurrence counts in homogeneous temporal buckets. */
public record WindowStatistics(
        int bucketCount,
        long totalOccurrences,
        double mean,
        double median,
        OptionalInt mode,
        int minimum,
        int maximum,
        double standardDeviation
) {
    public WindowStatistics {
        if (bucketCount <= 0 || totalOccurrences < 0 || minimum < 0 || maximum < minimum) throw new IllegalArgumentException("invalid window statistics");
        Objects.requireNonNull(mode);
        if (!Double.isFinite(mean) || !Double.isFinite(median) || !Double.isFinite(standardDeviation) || standardDeviation < 0) throw new IllegalArgumentException("invalid statistical value");
    }
}
