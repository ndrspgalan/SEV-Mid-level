package behavior.deviation.profile;

import behavior.expected.profile.PopulationStatistics;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * Descriptive distance between one observed value and its complete
 * profession-season population. It is evidence, not a risk or fraud verdict.
 */
public record BehaviorDeviation(
        double observedValue,
        double expectedMean,
        double expectedMedian,
        double absoluteDifferenceFromMean,
        double signedDifferenceFromMean,
        double signedDifferenceFromMedian,
        OptionalDouble relativeDifferenceFromMean,
        OptionalDouble percentileRank,
        OptionalDouble zScore
) {
    public BehaviorDeviation {
        Objects.requireNonNull(relativeDifferenceFromMean);
        Objects.requireNonNull(percentileRank);
        Objects.requireNonNull(zScore);
        double[] finite = {observedValue, expectedMean, expectedMedian, absoluteDifferenceFromMean,
                signedDifferenceFromMean, signedDifferenceFromMedian};
        for (double value : finite) if (!Double.isFinite(value)) throw new IllegalArgumentException("non-finite deviation value");
        if (observedValue < 0 || expectedMean < 0 || expectedMedian < 0 || absoluteDifferenceFromMean < 0)
            throw new IllegalArgumentException("negative magnitude");
        relativeDifferenceFromMean.ifPresent(v -> { if (!Double.isFinite(v)) throw new IllegalArgumentException("non-finite relative difference"); });
        percentileRank.ifPresent(v -> { if (!Double.isFinite(v) || v < 0 || v > 100) throw new IllegalArgumentException("invalid percentile rank"); });
        zScore.ifPresent(v -> { if (!Double.isFinite(v)) throw new IllegalArgumentException("non-finite z-score"); });
    }

    public static BehaviorDeviation compare(double observed, PopulationStatistics expected, OptionalDouble percentileRank) {
        Objects.requireNonNull(expected);
        double signedMean = observed - expected.mean();
        OptionalDouble relative = expected.mean() == 0d
                ? OptionalDouble.empty()
                : OptionalDouble.of(signedMean / expected.mean());
        OptionalDouble z = expected.populationStandardDeviation() == 0d
                ? OptionalDouble.empty()
                : OptionalDouble.of(signedMean / expected.populationStandardDeviation());
        return new BehaviorDeviation(observed, expected.mean(), expected.median(), Math.abs(signedMean), signedMean,
                observed - expected.median(), relative, percentileRank, z);
    }
}
