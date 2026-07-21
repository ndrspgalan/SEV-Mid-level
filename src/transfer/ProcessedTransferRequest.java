package transfer;

import transaction.TransactionId;

import java.time.Instant;
import java.util.Objects;

public record ProcessedTransferRequest(
        TransferRequest request,
        TransactionId transactionId,
        Instant occurredAt,
        TransferExecution execution
) {
    public ProcessedTransferRequest {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(execution, "execution must not be null");
    }
}
