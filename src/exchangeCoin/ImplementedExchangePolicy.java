package exchangeCoin;

import coinProperties.Currency;
import consumerRegistry.BankAccount;

import java.util.Objects;

public class ImplementedExchangePolicy implements ExchangePolicy {

    private static final int ROUTE_NOT_ALLOWED = -1;
    private static final int NOT_EXACTLY_CONVERTIBLE = -2;

    @Override
    public ExchangeResult exchange(
            BankAccount bankAccount,
            Currency sourceCurrency,
            Currency targetCurrency,
            int sourceQuantity
    ) {
        Objects.requireNonNull(bankAccount, "bankAccount must not be null");
        Objects.requireNonNull(sourceCurrency, "sourceCurrency must not be null");
        Objects.requireNonNull(targetCurrency, "targetCurrency must not be null");

        if (!bankAccount.isOperational()) {
            return ExchangeResult.rejected(ExchangeRejectionReason.ACCOUNT_NOT_OPERATIONAL);
        }

        if (sourceQuantity <= 0) {
            return ExchangeResult.rejected(
                    ExchangeRejectionReason.NON_POSITIVE_QUANTITY
            );
        }

        if (sourceCurrency == targetCurrency) {
            return ExchangeResult.rejected(
                    ExchangeRejectionReason.SAME_SOURCE_AND_TARGET_CURRENCY
            );
        }

        int targetQuantity = calculateTargetQuantity(
                sourceCurrency,
                targetCurrency,
                sourceQuantity
        );

        if (targetQuantity == ROUTE_NOT_ALLOWED) {
            return ExchangeResult.rejected(
                    ExchangeRejectionReason.EXCHANGE_ROUTE_NOT_ALLOWED
            );
        }

        if (targetQuantity == NOT_EXACTLY_CONVERTIBLE) {
            return ExchangeResult.rejected(
                    ExchangeRejectionReason.QUANTITY_NOT_EXACTLY_CONVERTIBLE
            );
        }

        if (!bankAccount.withdraw(sourceCurrency, sourceQuantity)) {
            return ExchangeResult.rejected(
                    ExchangeRejectionReason.INSUFFICIENT_BALANCE
            );
        }

        bankAccount.deposit(targetCurrency, targetQuantity);
        return ExchangeResult.accepted(targetQuantity);
    }

    private int calculateTargetQuantity(
            Currency sourceCurrency,
            Currency targetCurrency,
            int sourceQuantity
    ) {
        if (sourceCurrency == Currency.VALERITA && targetCurrency == Currency.SUELDO) {
            return divideExactly(sourceQuantity, 1_000);
        }
        if (sourceCurrency == Currency.SUELDO && targetCurrency == Currency.VALERITA) {
            return sourceQuantity * 1_000;
        }
        if (sourceCurrency == Currency.SUELDO && targetCurrency == Currency.BERYLARE) {
            return divideExactly(sourceQuantity, 210);
        }
        if (sourceCurrency == Currency.BERYLARE && targetCurrency == Currency.SUELDO) {
            return sourceQuantity * 210;
        }
        if (sourceCurrency == Currency.BERYLARE && targetCurrency == Currency.REAL_A5) {
            return divideExactly(sourceQuantity, 2);
        }
        if (sourceCurrency == Currency.REAL_A5 && targetCurrency == Currency.BERYLARE) {
            return sourceQuantity * 2;
        }
        return ROUTE_NOT_ALLOWED;
    }

    private int divideExactly(int quantity, int divisor) {
        if (quantity % divisor != 0) {
            return NOT_EXACTLY_CONVERTIBLE;
        }
        return quantity / divisor;
    }
}
