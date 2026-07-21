package institutional.snapshot;

import banking.identity.ConsumerId;
import banking.identity.Profession;
import behavior.temporal.SeasonPeriod;

import java.util.*;

/**
 * Institutional photograph of one profession during one concrete season.
 *
 * <p>The population is the complete set of distinct holders registered in the
 * profession at any instant of the bounded season. It is not a sample of a
 * larger hypothetical economy. This object is descriptive: it records
 * institutional movement but does not decide whether that movement is healthy,
 * risky or fraudulent.</p>
 */
public record PopulationSnapshot(
        Profession profession,
        SeasonPeriod seasonPeriod,
        Set<ConsumerId> registeredConsumers,
        long registrations,
        long professionChangesIn,
        long professionChangesOut,
        long holderAssignments,
        long holderReleases,
        long deaths,
        long accountClosures,
        long transfersSent,
        long transfersReceived,
        long upwardMobilityIn,
        long upwardMobilityOut,
        long downwardMobilityIn,
        long downwardMobilityOut
) {
    public PopulationSnapshot {
        Objects.requireNonNull(profession);
        Objects.requireNonNull(seasonPeriod);
        registeredConsumers = Collections.unmodifiableSet(new LinkedHashSet<>(Objects.requireNonNull(registeredConsumers)));
        long[] values = {registrations, professionChangesIn, professionChangesOut, holderAssignments,
                holderReleases, deaths, accountClosures, transfersSent, transfersReceived,
                upwardMobilityIn, upwardMobilityOut, downwardMobilityIn, downwardMobilityOut};
        for (long value : values) if (value < 0) throw new IllegalArgumentException("snapshot counters must not be negative");
    }

    public int populationSize() { return registeredConsumers.size(); }
    public long netProfessionMobility() { return professionChangesIn - professionChangesOut; }
    public long netUpwardMobility() { return upwardMobilityIn - upwardMobilityOut; }
    public long netDownwardMobility() { return downwardMobilityIn - downwardMobilityOut; }
}
