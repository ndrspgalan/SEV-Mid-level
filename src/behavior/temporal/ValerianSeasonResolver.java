package behavior.temporal;

import java.time.*;
import java.util.Objects;

/** Winter: Nov-Jan; Spring: Feb-Apr; Summer: May-Jul; Autumn: Aug-Oct. */
public final class ValerianSeasonResolver implements SeasonResolver {
    private final ZoneId zone;
    public ValerianSeasonResolver(ZoneId zone) { this.zone = Objects.requireNonNull(zone); }
    @Override public SeasonPeriod resolve(Instant instant) {
        LocalDate date = Objects.requireNonNull(instant).atZone(zone).toLocalDate();
        int y = date.getYear();
        return switch (date.getMonthValue()) {
            case 11, 12 -> new SeasonPeriod(Season.WINTER, y + 1, LocalDate.of(y, 11, 1), LocalDate.of(y + 1, 1, 31));
            case 1 -> new SeasonPeriod(Season.WINTER, y, LocalDate.of(y - 1, 11, 1), LocalDate.of(y, 1, 31));
            case 2, 3, 4 -> new SeasonPeriod(Season.SPRING, y, LocalDate.of(y, 2, 1), LocalDate.of(y, 4, 30));
            case 5, 6, 7 -> new SeasonPeriod(Season.SUMMER, y, LocalDate.of(y, 5, 1), LocalDate.of(y, 7, 31));
            default -> new SeasonPeriod(Season.AUTUMN, y, LocalDate.of(y, 8, 1), LocalDate.of(y, 10, 31));
        };
    }
    public ZoneId zone() { return zone; }
}
