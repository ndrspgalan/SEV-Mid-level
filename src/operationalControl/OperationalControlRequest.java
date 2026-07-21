package operationalControl;

import coinProperties.Currency;
import consumableRegistry.ConsumableType;
import consumerRegistry.BankAccount;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record OperationalControlRequest(
        BankAccount account,
        MonetaryOperationType operationType,
        Currency currency,
        int amount,
        Instant occurredAt,
        Optional<Currency> targetCurrency,
        Optional<ConsumableType> consumableType
) {
    public OperationalControlRequest {
        Objects.requireNonNull(account);
        Objects.requireNonNull(operationType);
        Objects.requireNonNull(currency);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(targetCurrency);
        Objects.requireNonNull(consumableType);
        if (amount <= 0) throw new IllegalArgumentException("amount must be positive");
    }

    public OperationalControlRequest(BankAccount account, MonetaryOperationType operationType,
            Currency currency, int amount, Instant occurredAt) {
        this(account, operationType, currency, amount, occurredAt, Optional.empty(), Optional.empty());
    }

    public static OperationalControlRequest exchange(BankAccount account, Currency source,
            Currency target, int amount, Instant occurredAt) {
        return new OperationalControlRequest(account, MonetaryOperationType.EXCHANGE, source, amount,
                occurredAt, Optional.of(target), Optional.empty());
    }

    public static OperationalControlRequest commercial(BankAccount account,
            MonetaryOperationType operationType, Currency currency, ConsumableType type,
            int amount, Instant occurredAt) {
        return new OperationalControlRequest(account, operationType, currency, amount, occurredAt,
                Optional.empty(), Optional.of(type));
    }
}
