package transfer;

import consumerRegistry.BankAccount;

public interface TransferPolicy {
    TransferExecution transfer(
            BankAccount source,
            BankAccount destination,
            TransferRequest request
    );
}
