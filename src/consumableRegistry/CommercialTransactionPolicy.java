package consumableRegistry;

import consumerRegistry.BankAccount;

public interface CommercialTransactionPolicy {

    CommercialTransactionResult purchase(
            BankAccount buyerAccount,
            BankAccount sellerAccount,
            Consumable consumable
    );
}
