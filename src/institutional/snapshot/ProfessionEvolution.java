package institutional.snapshot;

import banking.identity.Profession;
import behavior.temporal.SeasonPeriod;

import java.util.Objects;

/** Direct comparison between two consecutive photographs of the same profession. */
public record ProfessionEvolution(
        Profession profession,
        SeasonPeriod previousSeason,
        SeasonPeriod currentSeason,
        int previousPopulation,
        int currentPopulation,
        long populationDelta,
        long transferBalanceDelta,
        long upwardMobilityBalanceDelta,
        long downwardMobilityBalanceDelta,
        long deathDelta,
        long holderReleaseDelta
) {
    public ProfessionEvolution {
        Objects.requireNonNull(profession); Objects.requireNonNull(previousSeason); Objects.requireNonNull(currentSeason);
    }
}
