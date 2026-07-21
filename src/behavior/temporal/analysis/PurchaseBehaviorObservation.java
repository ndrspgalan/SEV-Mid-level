package behavior.temporal.analysis;

import coinProperties.Currency;

import java.time.Instant;
import java.util.Objects;

/** Frozen normalized purchase values used only for longitudinal tracking. */
record PurchaseBehaviorObservation(Instant occurredAt, int quantity, Currency currency, int totalPrice) {
    PurchaseBehaviorObservation {
        Objects.requireNonNull(occurredAt); Objects.requireNonNull(currency);
        if (quantity <= 0 || totalPrice <= 0) throw new IllegalArgumentException("purchase magnitudes must be positive");
    }
}
