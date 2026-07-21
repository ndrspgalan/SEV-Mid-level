package behavior.temporal;

import java.time.LocalDate;
import java.util.Objects;

/** One concrete Valerian season. Winter is anchored to the year containing January. */
public record SeasonPeriod(Season season, int anchorYear, LocalDate startsOn, LocalDate endsOn) {
    public SeasonPeriod {
        Objects.requireNonNull(season); Objects.requireNonNull(startsOn); Objects.requireNonNull(endsOn);
        if (endsOn.isBefore(startsOn)) throw new IllegalArgumentException("season end precedes start");
    }
    public String label() { return season.code() + "-" + anchorYear; }
}
