package behavior.evidence.casefile;

import behavior.deviation.profile.BehaviorDeviation;
import behavior.expected.profile.ExpectedBehaviorMetric;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * One immutable entry in an analytical case file.
 *
 * <p>It preserves the complete M3.2 deviation and adds only organization and
 * descriptive magnitude coordinates. It does not assign risk, suspicion,
 * relevance, priority or fraud meaning.</p>
 */
public record BehaviorEvidence(
        ExpectedBehaviorMetric metric,
        BehaviorEvidenceCategory category,
        BehaviorDeviation deviation,
        DeviationDirection direction,
        OptionalDouble standardizedMagnitude,
        OptionalDouble percentileExtremity
) {
    public BehaviorEvidence {
        Objects.requireNonNull(metric); Objects.requireNonNull(category);
        Objects.requireNonNull(deviation); Objects.requireNonNull(direction);
        Objects.requireNonNull(standardizedMagnitude); Objects.requireNonNull(percentileExtremity);
        standardizedMagnitude.ifPresent(v -> { if (!Double.isFinite(v) || v < 0) throw new IllegalArgumentException("invalid standardized magnitude"); });
        percentileExtremity.ifPresent(v -> { if (!Double.isFinite(v) || v < 0 || v > 50) throw new IllegalArgumentException("invalid percentile extremity"); });
        DeviationDirection expected = deviation.signedDifferenceFromMean() < 0 ? DeviationDirection.BELOW_REFERENCE
                : deviation.signedDifferenceFromMean() > 0 ? DeviationDirection.ABOVE_REFERENCE : DeviationDirection.AT_REFERENCE;
        if (direction != expected) throw new IllegalArgumentException("deviation direction mismatch");
    }

    public static BehaviorEvidence of(ExpectedBehaviorMetric metric, BehaviorEvidenceCategory category, BehaviorDeviation deviation) {
        DeviationDirection direction = deviation.signedDifferenceFromMean() < 0 ? DeviationDirection.BELOW_REFERENCE
                : deviation.signedDifferenceFromMean() > 0 ? DeviationDirection.ABOVE_REFERENCE : DeviationDirection.AT_REFERENCE;
        OptionalDouble standardized = deviation.zScore().isPresent()
                ? OptionalDouble.of(Math.abs(deviation.zScore().getAsDouble())) : OptionalDouble.empty();
        OptionalDouble extremity = deviation.percentileRank().isPresent()
                ? OptionalDouble.of(Math.abs(deviation.percentileRank().getAsDouble() - 50d)) : OptionalDouble.empty();
        return new BehaviorEvidence(metric, category, deviation, direction, standardized, extremity);
    }
}
