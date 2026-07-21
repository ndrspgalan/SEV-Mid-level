package behavior.temporal;

/**
 * Fixed analytical scales accepted by SEV Mid.
 *
 * <p>The season is the enclosing reference period, not a statistical bucket:
 * seasonal totals are represented by {@code SeasonActivitySummary}. This avoids
 * manufacturing a zero deviation from a single seasonal value.</p>
 */
public enum ObservationWindow {
    SAME_DAY,
    EIGHT_HOUR_PERIOD,
    WEEK,
    FOUR_WEEK_MONTH
}
