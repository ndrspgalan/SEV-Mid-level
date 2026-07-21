package economicEvent;

import coinProperties.Currency;

import java.util.Objects;

/** Non-negative amount expressed in one Valerian currency denomination. */
public record EconomicAmount(Currency currency, int amount) {
    public EconomicAmount {
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount < 0) throw new IllegalArgumentException("economic amount must not be negative");
    }
}
