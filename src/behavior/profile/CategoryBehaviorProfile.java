package behavior.profile;

import coinProperties.Currency;
import consumableRegistry.ConsumableCategory;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Completed-purchase aggregate for one functional consumable category. */
public record CategoryBehaviorProfile(
        ConsumableCategory category,
        long purchaseCount,
        long unitsPurchased,
        long distinctConsumables,
        Map<Currency, Integer> totalSpentByCurrency
) {
    public CategoryBehaviorProfile {
        Objects.requireNonNull(category, "category must not be null");
        if (purchaseCount <= 0 || unitsPurchased <= 0 || distinctConsumables <= 0) {
            throw new IllegalArgumentException("category aggregate counters must be positive");
        }
        Objects.requireNonNull(totalSpentByCurrency, "total spent must not be null");
        EnumMap<Currency, Integer> copy = new EnumMap<>(Currency.class);
        totalSpentByCurrency.forEach((currency, amount) -> {
            Objects.requireNonNull(currency);
            Objects.requireNonNull(amount);
            if (amount < 0) throw new IllegalArgumentException("total spent must not be negative");
            copy.put(currency, amount);
        });
        if (copy.isEmpty()) throw new IllegalArgumentException("total spent must not be empty");
        totalSpentByCurrency = Collections.unmodifiableMap(copy);
    }
}
