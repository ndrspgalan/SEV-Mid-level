package behavior.temporal.analysis;

import behavior.temporal.*;
import behavior.temporal.profile.*;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Calculates the same compact descriptive statistic set at each fixed SEV scale.
 *
 * <p>The calculator measures magnitude per fixed bucket, not elapsed time between
 * events. Each statistic is kept for the phenomenon it describes best: central
 * tendency, repeated value, extremes and population dispersion. Interpretation
 * belongs to later collective and risk layers.</p>
 */
public final class TemporalStatisticsCalculator {
    private final ZoneId zone;
    public TemporalStatisticsCalculator(ZoneId zone) { this.zone = Objects.requireNonNull(zone); }

    public TemporalBehaviorStatistics calculate(List<Instant> occurrences, SeasonPeriod season) {
        Objects.requireNonNull(occurrences);
        List<TimedValue> values = occurrences.stream().map(value -> new TimedValue(value, 1)).toList();
        return calculateWeighted(values, season);
    }

    public TemporalBehaviorStatistics calculateWeighted(List<TimedValue> values, SeasonPeriod season) {
        Objects.requireNonNull(values); Objects.requireNonNull(season);
        if (values.isEmpty()) throw new IllegalArgumentException("values must not be empty");
        EnumMap<ObservationWindow, WindowStatistics> result = new EnumMap<>(ObservationWindow.class);
        result.put(ObservationWindow.SAME_DAY, stats(countByFixedDays(values, season, 1)));
        result.put(ObservationWindow.EIGHT_HOUR_PERIOD, stats(countByEightHours(values, season)));
        result.put(ObservationWindow.WEEK, stats(countByFixedDays(values, season, 7)));
        result.put(ObservationWindow.FOUR_WEEK_MONTH, stats(countByFixedDays(values, season, 28)));
        return new TemporalBehaviorStatistics(result);
    }

    private int[] countByEightHours(List<TimedValue> values, SeasonPeriod season) {
        int days = seasonDays(season);
        int[] counts = new int[days * 3];
        for (TimedValue value : values) {
            ZonedDateTime z = value.occurredAt().atZone(zone);
            int day = (int)ChronoUnit.DAYS.between(season.startsOn(), z.toLocalDate());
            int slot = z.getHour() / 8;
            if (day >= 0 && day < days) counts[day * 3 + slot] = Math.addExact(counts[day * 3 + slot], value.value());
        }
        return counts;
    }

    private int[] countByFixedDays(List<TimedValue> values, SeasonPeriod season, int width) {
        int days = seasonDays(season);
        int[] counts = new int[(days + width - 1) / width];
        for (TimedValue value : values) {
            LocalDate date = value.occurredAt().atZone(zone).toLocalDate();
            int day = (int)ChronoUnit.DAYS.between(season.startsOn(), date);
            if (day >= 0 && day < days) counts[day / width] = Math.addExact(counts[day / width], value.value());
        }
        return counts;
    }

    private int seasonDays(SeasonPeriod season) {
        return (int)ChronoUnit.DAYS.between(season.startsOn(), season.endsOn()) + 1;
    }

    static WindowStatistics stats(int[] values) {
        if (values.length == 0) throw new IllegalArgumentException("statistical population must not be empty");
        int total = Arrays.stream(values).sum();
        double mean = total / (double) values.length;
        int[] sorted = values.clone(); Arrays.sort(sorted);
        double median = sorted.length % 2 == 1 ? sorted[sorted.length/2] : (sorted[sorted.length/2-1] + sorted[sorted.length/2]) / 2.0;
        Map<Integer,Integer> freq = new HashMap<>();
        for (int v : values) freq.merge(v, 1, Integer::sum);
        int highest = freq.values().stream().max(Integer::compareTo).orElse(1);
        List<Integer> modes = freq.entrySet().stream().filter(e -> e.getValue() == highest).map(Map.Entry::getKey).toList();
        OptionalInt mode = highest > 1 && modes.size() == 1 ? OptionalInt.of(modes.get(0)) : OptionalInt.empty();
        double variance = 0;
        for (int v : values) { double d=v-mean; variance += d*d; }
        variance /= values.length; // complete SEV population for the bounded period
        return new WindowStatistics(values.length,total,mean,median,mode,sorted[0],sorted[sorted.length-1],Math.sqrt(variance));
    }
}
