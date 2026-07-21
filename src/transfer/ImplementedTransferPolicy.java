package transfer;

import consumerRegistry.BankAccount;

import java.util.Objects;
import java.util.Optional;

public final class ImplementedTransferPolicy implements TransferPolicy {

    @Override
    public synchronized TransferExecution transfer(
            BankAccount source,
            BankAccount destination,
            TransferRequest request
    ) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(destination, "destination must not be null");
        Objects.requireNonNull(request, "request must not be null");

        int sourceBefore = source.getBalance(request.currency());
        int destinationBefore = destination.getBalance(request.currency());

        if (!source.isOperational()) {
            return TransferExecution.rejected(
                    TransferRejectionReason.SOURCE_ACCOUNT_NOT_OPERATIONAL,
                    Optional.of(sourceBefore), Optional.of(destinationBefore)
            );
        }
        if (!destination.isOperational()) {
            return TransferExecution.rejected(
                    TransferRejectionReason.DESTINATION_ACCOUNT_NOT_OPERATIONAL,
                    Optional.of(sourceBefore), Optional.of(destinationBefore)
            );
        }

        if (source == destination) {
            return TransferExecution.rejected(
                    TransferRejectionReason.SAME_SOURCE_AND_DESTINATION_ACCOUNT,
                    Optional.of(sourceBefore),
                    Optional.of(destinationBefore)
            );
        }
        if (request.quantity() <= 0) {
            return TransferExecution.rejected(
                    TransferRejectionReason.NON_POSITIVE_QUANTITY,
                    Optional.of(sourceBefore),
                    Optional.of(destinationBefore)
            );
        }
        if (sourceBefore < request.quantity()) {
            return TransferExecution.rejected(
                    TransferRejectionReason.INSUFFICIENT_BALANCE,
                    Optional.of(sourceBefore),
                    Optional.of(destinationBefore)
            );
        }

        boolean withdrawn = source.withdraw(request.currency(), request.quantity());
        if (!withdrawn) {
            throw new IllegalStateException("validated transfer could not debit source account");
        }

        try {
            destination.deposit(request.currency(), request.quantity());
        } catch (RuntimeException exception) {
            source.deposit(request.currency(), request.quantity());
            throw exception;
        }

        int sourceAfter = source.getBalance(request.currency());
        int destinationAfter = destination.getBalance(request.currency());
        if (sourceBefore + destinationBefore != sourceAfter + destinationAfter) {
            throw new IllegalStateException("transfer violated monetary conservation");
        }

        return TransferExecution.completed(
                sourceBefore,
                sourceAfter,
                destinationBefore,
                destinationAfter
        );
    }
}
