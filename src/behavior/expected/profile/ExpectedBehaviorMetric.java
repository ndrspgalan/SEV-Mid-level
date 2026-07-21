package behavior.expected.profile;

import behavior.temporal.ObservationWindow;
import coinProperties.Currency;
import java.util.Objects;
import java.util.Optional;

/**
 * Typed identity of one collective metric. Subject values come from canonical
 * domain codes (event type, consumable id, category or day period), never from
 * interpretive labels such as "high" or "suspicious".
 */
public record ExpectedBehaviorMetric(
        ExpectedBehaviorMetricFamily family,
        String subject,
        Optional<Currency> currency,
        Optional<ObservationWindow> observationWindow
) implements Comparable<ExpectedBehaviorMetric> {
    public ExpectedBehaviorMetric {
        Objects.requireNonNull(family);
        subject = Objects.requireNonNull(subject).trim();
        if (subject.isEmpty()) throw new IllegalArgumentException("metric subject must not be blank");
        currency = Objects.requireNonNull(currency);
        observationWindow = Objects.requireNonNull(observationWindow);
    }
    public static ExpectedBehaviorMetric of(ExpectedBehaviorMetricFamily family, String subject) {
        return new ExpectedBehaviorMetric(family, subject, Optional.empty(), Optional.empty());
    }
    public static ExpectedBehaviorMetric windowed(ExpectedBehaviorMetricFamily family, String subject, ObservationWindow window) {
        return new ExpectedBehaviorMetric(family, subject, Optional.empty(), Optional.of(window));
    }
    public static ExpectedBehaviorMetric monetary(ExpectedBehaviorMetricFamily family, String subject, Currency currency) {
        return new ExpectedBehaviorMetric(family, subject, Optional.of(currency), Optional.empty());
    }
    public static ExpectedBehaviorMetric monetaryWindowed(ExpectedBehaviorMetricFamily family, String subject, Currency currency, ObservationWindow window) {
        return new ExpectedBehaviorMetric(family, subject, Optional.of(currency), Optional.of(window));
    }
    public String canonicalKey() {
        return family + "|" + subject + "|" + currency.map(Enum::name).orElse("-") + "|" + observationWindow.map(Enum::name).orElse("-");
    }
    @Override public int compareTo(ExpectedBehaviorMetric other) { return canonicalKey().compareTo(other.canonicalKey()); }
}
