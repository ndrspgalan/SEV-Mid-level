package behavior.expected.profile;

import banking.identity.Profession;
import behavior.temporal.SeasonPeriod;
import java.util.*;

/**
 * Empirical collective reference for a complete profession-season population.
 * "Expected" means observed distribution, not mandatory, legitimate or safe.
 */
public record ExpectedBehaviorSet(
        ExpectedBehaviorSetId id,
        Profession profession,
        SeasonPeriod seasonPeriod,
        int populationSize,
        Map<ExpectedBehaviorMetric, PopulationStatistics> metrics
) {
    public ExpectedBehaviorSet {
        Objects.requireNonNull(id); Objects.requireNonNull(profession); Objects.requireNonNull(seasonPeriod);
        if(populationSize<0)throw new IllegalArgumentException("negative population size");
        if(!id.equals(ExpectedBehaviorSetId.of(profession.code(),seasonPeriod)))throw new IllegalArgumentException("expected behavior identity mismatch");
        TreeMap<ExpectedBehaviorMetric,PopulationStatistics> copy=new TreeMap<>();
        Objects.requireNonNull(metrics).forEach((k,v)->copy.put(Objects.requireNonNull(k),Objects.requireNonNull(v)));
        if(populationSize==0 && !copy.isEmpty())throw new IllegalArgumentException("empty populations cannot expose artificial statistics");
        for(PopulationStatistics value:copy.values())if(value.populationSize()!=populationSize)throw new IllegalArgumentException("metric population mismatch");
        metrics=Collections.unmodifiableMap(copy);
    }
}
