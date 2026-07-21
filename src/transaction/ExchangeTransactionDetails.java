package transaction;

import coinProperties.Currency;

import java.util.Objects;
import java.util.Optional;

public record ExchangeTransactionDetails(
        String consumerId,
        Currency sourceCurrency,
        Currency targetCurrency,
        int sourceQuantity,
        Optional<Integer> targetQuantity,
        Optional<Integer> sourceBalanceBefore,
        Optional<Integer> sourceBalanceAfter,
        Optional<Integer> targetBalanceBefore,
        Optional<Integer> targetBalanceAfter,
        Optional<String> rejectionCode
) implements TransactionDetails {

    public ExchangeTransactionDetails {
        consumerId = requireText(consumerId, "consumerId");
        Objects.requireNonNull(sourceCurrency, "sourceCurrency must not be null");
        Objects.requireNonNull(targetCurrency, "targetCurrency must not be null");
        targetQuantity = Objects.requireNonNull(targetQuantity);
        sourceBalanceBefore = Objects.requireNonNull(sourceBalanceBefore);
        sourceBalanceAfter = Objects.requireNonNull(sourceBalanceAfter);
        targetBalanceBefore = Objects.requireNonNull(targetBalanceBefore);
        targetBalanceAfter = Objects.requireNonNull(targetBalanceAfter);
        rejectionCode = Objects.requireNonNull(rejectionCode);
    }

    @Override
    public String summary() {
        if (rejectionCode.isPresent()) {
            return "Cambio de " + sourceQuantity + " " + sourceCurrency
                    + " por " + targetCurrency + " rechazado: " + rejectionCode.get();
        }
        return "Cambio de " + sourceQuantity + " " + sourceCurrency
                + " por " + targetQuantity.orElseThrow() + " " + targetCurrency;
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
