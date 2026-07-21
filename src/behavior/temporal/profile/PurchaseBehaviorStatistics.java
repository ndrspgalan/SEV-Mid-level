package behavior.temporal.profile;

import coinProperties.Currency;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Independent purchase magnitudes. Monetary currencies remain separated and are
 * never added through an implicit conversion policy.
 */
public record PurchaseBehaviorStatistics(
        TemporalBehaviorStatistics purchases,
        TemporalBehaviorStatistics units,
        Map<Currency, TemporalBehaviorStatistics> monetaryVolume
) {
    public PurchaseBehaviorStatistics {
        Objects.requireNonNull(purchases); Objects.requireNonNull(units); Objects.requireNonNull(monetaryVolume);
        EnumMap<Currency, TemporalBehaviorStatistics> copy = new EnumMap<>(Currency.class);
        monetaryVolume.forEach((currency, statistics) -> copy.put(Objects.requireNonNull(currency), Objects.requireNonNull(statistics)));
        monetaryVolume = Collections.unmodifiableMap(copy);
    }
}
