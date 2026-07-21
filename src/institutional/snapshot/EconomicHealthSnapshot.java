package institutional.snapshot;

import behavior.temporal.SeasonPeriod;

import java.util.*;

/**
 * Descriptive economic-health view. It supports SLA and institutional
 * observability without collapsing operational health into risk or fraud.
 */
public record EconomicHealthSnapshot(
        SeasonPeriod currentSeason,
        Optional<SeasonPeriod> previousSeason,
        List<ProfessionEvolution> professionEvolution,
        List<SeasonSnapshot> comparisonTable
) {
    public EconomicHealthSnapshot {
        Objects.requireNonNull(currentSeason);
        previousSeason = Objects.requireNonNull(previousSeason);
        professionEvolution = List.copyOf(Objects.requireNonNull(professionEvolution));
        comparisonTable = List.copyOf(Objects.requireNonNull(comparisonTable));
        if (comparisonTable.size() > 5) throw new IllegalArgumentException("comparison table is limited to previous year plus current season");
    }
}
