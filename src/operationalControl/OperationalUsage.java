package operationalControl;

import banking.identity.BankAccountId;
import coinProperties.Currency;

import java.time.Instant;
import java.util.Objects;
public record OperationalUsage(BankAccountId accountId, MonetaryOperationType operationType, Currency currency,
 LimitWindow window, Instant periodStart, Instant periodEndExclusive, long accumulatedAmount, int operationCount) {
 public OperationalUsage { Objects.requireNonNull(accountId);Objects.requireNonNull(operationType);Objects.requireNonNull(currency);Objects.requireNonNull(window);Objects.requireNonNull(periodStart);Objects.requireNonNull(periodEndExclusive); }
}
