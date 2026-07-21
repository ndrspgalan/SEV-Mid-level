package exchangeCoin;

import coinProperties.Currency;
import consumerRegistry.BankAccount;

public interface ExchangePolicy {

    ExchangeResult exchange(
            BankAccount bankAccount,
            Currency sourceCurrency,
            Currency targetCurrency,
            int sourceQuantity
    );
}
