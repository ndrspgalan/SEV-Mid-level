package behavior.profile;

import coinProperties.Currency;
import consumableRegistry.ConsumableCategory;

import java.time.Instant;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Objective aggregate of completed purchases for one consumable. No temporal pattern is inferred here. */
public record ConsumableBehaviorProfile(
        String consumableId,
        String consumableName,
        ConsumableCategory category,
        long purchaseCount,
        long unitsPurchased,
        Map<Currency, Integer> totalSpentByCurrency,
        Map<Currency, Integer> minimumUnitPriceByCurrency,
        Map<Currency, Integer> maximumUnitPriceByCurrency,
        Instant firstPurchaseAt,
        Instant lastPurchaseAt
) {
    public ConsumableBehaviorProfile {
        consumableId = text(consumableId, "consumable id");
        consumableName = text(consumableName, "consumable name");
        Objects.requireNonNull(category, "category must not be null");
        if (purchaseCount <= 0) throw new IllegalArgumentException("purchase count must be positive");
        if (unitsPurchased <= 0) throw new IllegalArgumentException("units purchased must be positive");
        totalSpentByCurrency = immutableMoney(totalSpentByCurrency, "total spent");
        minimumUnitPriceByCurrency = immutableMoney(minimumUnitPriceByCurrency, "minimum unit price");
        maximumUnitPriceByCurrency = immutableMoney(maximumUnitPriceByCurrency, "maximum unit price");
        Objects.requireNonNull(firstPurchaseAt, "first purchase must not be null");
        Objects.requireNonNull(lastPurchaseAt, "last purchase must not be null");
        if (lastPurchaseAt.isBefore(firstPurchaseAt)) throw new IllegalArgumentException("last purchase precedes first purchase");
        if (!totalSpentByCurrency.keySet().equals(minimumUnitPriceByCurrency.keySet())
                || !totalSpentByCurrency.keySet().equals(maximumUnitPriceByCurrency.keySet())) {
            throw new IllegalArgumentException("all monetary maps must contain the same currencies");
        }
        for (Currency currency : totalSpentByCurrency.keySet()) {
            if (minimumUnitPriceByCurrency.get(currency) > maximumUnitPriceByCurrency.get(currency)) {
                throw new IllegalArgumentException("minimum unit price exceeds maximum unit price for " + currency);
            }
        }
    }

    public double averageUnitsPerPurchase() { return (double) unitsPurchased / purchaseCount; }

    public double averageUnitPrice(Currency currency) {
        Integer total = totalSpentByCurrency.get(Objects.requireNonNull(currency));
        if (total == null) throw new IllegalArgumentException("currency not present: " + currency);
        return (double) total / unitsPurchased;
    }

    private static String text(String value, String label) {
        Objects.requireNonNull(value, label + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return normalized;
    }

    private static Map<Currency, Integer> immutableMoney(Map<Currency, Integer> values, String label) {
        Objects.requireNonNull(values, label + " must not be null");
        EnumMap<Currency, Integer> copy = new EnumMap<>(Currency.class);
        values.forEach((currency, amount) -> {
            Objects.requireNonNull(currency, label + " currency must not be null");
            Objects.requireNonNull(amount, label + " amount must not be null");
            if (amount < 0) throw new IllegalArgumentException(label + " amount must not be negative");
            copy.put(currency, amount);
        });
        if (copy.isEmpty()) throw new IllegalArgumentException(label + " must not be empty");
        return Collections.unmodifiableMap(copy);
    }
}
