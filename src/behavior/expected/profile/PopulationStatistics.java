package behavior.expected.profile;

import java.util.Objects;
import java.util.OptionalDouble;

/** Statistics over the complete profession-season population, including zeros. */
public record PopulationStatistics(
        int populationSize,
        int activeMemberCount,
        int inactiveMemberCount,
        double minimum,
        double maximum,
        double mean,
        double median,
        OptionalDouble mode,
        double populationStandardDeviation,
        double percentile25,
        double percentile50,
        double percentile75,
        double interquartileRange
) {
    public PopulationStatistics {
        if (populationSize <= 0 || activeMemberCount < 0 || inactiveMemberCount < 0 || activeMemberCount + inactiveMemberCount != populationSize)
            throw new IllegalArgumentException("invalid population cardinality");
        Objects.requireNonNull(mode);
        double[] values={minimum,maximum,mean,median,populationStandardDeviation,percentile25,percentile50,percentile75,interquartileRange};
        for(double value:values) if(!Double.isFinite(value)) throw new IllegalArgumentException("non-finite population statistic");
        if(minimum<0 || maximum<minimum || populationStandardDeviation<0 || interquartileRange<0)
            throw new IllegalArgumentException("invalid population statistic");
    }
}
