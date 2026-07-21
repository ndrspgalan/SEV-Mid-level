package behavior.evidence.casefile;

import banking.identity.Profession;
import behavior.temporal.SeasonPeriod;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * Frozen institutional coordinates surrounding a case file.
 * Values are descriptive and must not be read as risk multipliers.
 */
public record InstitutionalContext(
        Profession profession,
        SeasonPeriod seasonPeriod,
        int populationSize,
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
        long downwardMobilityOut,
        OptionalLong populationDeltaFromPreviousSeason,
        OptionalLong transferBalanceDeltaFromPreviousSeason,
        OptionalLong upwardMobilityBalanceDeltaFromPreviousSeason,
        OptionalLong downwardMobilityBalanceDeltaFromPreviousSeason
) {
    public InstitutionalContext {
        Objects.requireNonNull(profession); Objects.requireNonNull(seasonPeriod);
        Objects.requireNonNull(populationDeltaFromPreviousSeason); Objects.requireNonNull(transferBalanceDeltaFromPreviousSeason);
        Objects.requireNonNull(upwardMobilityBalanceDeltaFromPreviousSeason); Objects.requireNonNull(downwardMobilityBalanceDeltaFromPreviousSeason);
        if (populationSize < 0) throw new IllegalArgumentException("negative population");
        long[] counters={registrations,professionChangesIn,professionChangesOut,holderAssignments,holderReleases,deaths,accountClosures,
                transfersSent,transfersReceived,upwardMobilityIn,upwardMobilityOut,downwardMobilityIn,downwardMobilityOut};
        for(long value:counters) if(value<0) throw new IllegalArgumentException("negative institutional counter");
    }
    public long transferBalance(){return transfersReceived-transfersSent;}
    public long netUpwardMobility(){return upwardMobilityIn-upwardMobilityOut;}
    public long netDownwardMobility(){return downwardMobilityIn-downwardMobilityOut;}
}
