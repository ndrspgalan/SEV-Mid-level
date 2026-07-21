package consumableRegistry;

import coinProperties.Currency;
import consumerRegistry.BankAccount;

import java.util.Objects;

public class ImplementedCommercialTransactionPolicy
        implements CommercialTransactionPolicy {

    @Override
    public CommercialTransactionResult purchase(
            BankAccount buyerAccount,
            BankAccount sellerAccount,
            Consumable consumable
    ) {
        Objects.requireNonNull(buyerAccount, "buyerAccount must not be null");
        Objects.requireNonNull(sellerAccount, "sellerAccount must not be null");
        Objects.requireNonNull(consumable, "consumable must not be null");

        if (!buyerAccount.isOperational()) {
            return CommercialTransactionResult.rejected(
                    CommercialTransactionRejectionReason.BUYER_ACCOUNT_NOT_OPERATIONAL
            );
        }
        if (!sellerAccount.isOperational()) {
            return CommercialTransactionResult.rejected(
                    CommercialTransactionRejectionReason.SELLER_ACCOUNT_NOT_OPERATIONAL
            );
        }

        if (buyerAccount == sellerAccount) {
            return CommercialTransactionResult.rejected(
                    CommercialTransactionRejectionReason.SAME_BUYER_AND_SELLER_ACCOUNT
            );
        }

        Currency currency = consumable.getPriceCurrency();
        int price = consumable.getPrice();

        if (price <= 0) {
            return CommercialTransactionResult.rejected(
                    CommercialTransactionRejectionReason.NON_POSITIVE_PRICE
            );
        }

        if (!isCurrencyAllowed(consumable.getType(), currency)) {
            return CommercialTransactionResult.rejected(
                    CommercialTransactionRejectionReason.CURRENCY_NOT_ALLOWED_FOR_CONSUMABLE_TYPE
            );
        }

        if (!buyerAccount.withdraw(currency, price)) {
            return CommercialTransactionResult.rejected(
                    CommercialTransactionRejectionReason.INSUFFICIENT_BUYER_BALANCE
            );
        }

        sellerAccount.deposit(currency, price);
        return CommercialTransactionResult.accepted();
    }

    private boolean isCurrencyAllowed(
            ConsumableType type,
            Currency currency
    ) {
        return switch (type) {
            case BASIC_NECESSITY ->
                    currency == Currency.VALERITA || currency == Currency.SUELDO;
            case SOCIAL_UTILITY ->
                    currency == Currency.VALERITA
                            || currency == Currency.SUELDO
                            || currency == Currency.BERYLARE;
            case PRIVATE_USE -> true;
        };
    }
}
