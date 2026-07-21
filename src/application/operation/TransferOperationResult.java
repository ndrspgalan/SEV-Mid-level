package application.operation;

import coinProperties.Currency;
import transaction.TransactionId;
import transfer.TransferRejectionReason;
import transfer.TransferRequest;
import transfer.TransferRequestId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class TransferOperationResult {

    public enum Outcome {
        COMPLETED,
        REJECTED,
        IDEMPOTENT_REPLAY,
        IDEMPOTENCY_CONFLICT
    }

    private final Outcome outcome;
    private final TransferRequestId requestId;
    private final TransactionId transactionId;
    private final Instant occurredAt;
    private final String sourceConsumerId;
    private final String destinationConsumerId;
    private final Currency currency;
    private final int quantity;
    private final String reference;
    private final Integer sourceBalanceBefore;
    private final Integer sourceBalanceAfter;
    private final Integer destinationBalanceBefore;
    private final Integer destinationBalanceAfter;
    private final TransferRejectionReason rejectionReason;

    private TransferOperationResult(
            Outcome outcome,
            TransferRequest request,
            TransactionId transactionId,
            Instant occurredAt,
            Integer sourceBalanceBefore,
            Integer sourceBalanceAfter,
            Integer destinationBalanceBefore,
            Integer destinationBalanceAfter,
            TransferRejectionReason rejectionReason
    ) {
        this.outcome = Objects.requireNonNull(outcome);
        Objects.requireNonNull(request);
        this.requestId = request.requestId();
        this.transactionId = Objects.requireNonNull(transactionId);
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.sourceConsumerId = request.sourceConsumerId();
        this.destinationConsumerId = request.destinationConsumerId();
        this.currency = request.currency();
        this.quantity = request.quantity();
        this.reference = request.reference();
        this.sourceBalanceBefore = sourceBalanceBefore;
        this.sourceBalanceAfter = sourceBalanceAfter;
        this.destinationBalanceBefore = destinationBalanceBefore;
        this.destinationBalanceAfter = destinationBalanceAfter;
        this.rejectionReason = rejectionReason;
    }

    public static TransferOperationResult completed(
            TransferRequest request,
            TransactionId transactionId,
            Instant occurredAt,
            int sourceBefore,
            int sourceAfter,
            int destinationBefore,
            int destinationAfter
    ) {
        return new TransferOperationResult(
                Outcome.COMPLETED, request, transactionId, occurredAt,
                sourceBefore, sourceAfter, destinationBefore, destinationAfter,
                null
        );
    }

    public static TransferOperationResult rejected(
            TransferRequest request,
            TransactionId transactionId,
            Instant occurredAt,
            TransferRejectionReason reason,
            Optional<Integer> sourceBefore,
            Optional<Integer> destinationBefore
    ) {
        return new TransferOperationResult(
                Outcome.REJECTED, request, transactionId, occurredAt,
                sourceBefore.orElse(null), null,
                destinationBefore.orElse(null), null,
                Objects.requireNonNull(reason)
        );
    }

    public TransferOperationResult asIdempotentReplay() {
        if (outcome == Outcome.IDEMPOTENCY_CONFLICT) {
            throw new IllegalStateException("conflict cannot be replayed");
        }
        return new TransferOperationResult(
                Outcome.IDEMPOTENT_REPLAY,
                toRequest(),
                transactionId,
                occurredAt,
                sourceBalanceBefore,
                sourceBalanceAfter,
                destinationBalanceBefore,
                destinationBalanceAfter,
                rejectionReason
        );
    }

    public static TransferOperationResult idempotencyConflict(
            TransferRequest attempted,
            TransactionId originalTransactionId,
            Instant originalOccurredAt
    ) {
        return new TransferOperationResult(
                Outcome.IDEMPOTENCY_CONFLICT,
                attempted,
                originalTransactionId,
                originalOccurredAt,
                null, null, null, null, null
        );
    }

    private TransferRequest toRequest() {
        return new TransferRequest(
                requestId,
                sourceConsumerId,
                destinationConsumerId,
                currency,
                quantity,
                reference
        );
    }

    public Outcome getOutcome() { return outcome; }
    public boolean isCompleted() { return outcome == Outcome.COMPLETED || (outcome == Outcome.IDEMPOTENT_REPLAY && rejectionReason == null); }
    public boolean isRejected() { return outcome == Outcome.REJECTED || (outcome == Outcome.IDEMPOTENT_REPLAY && rejectionReason != null); }
    public boolean isIdempotentReplay() { return outcome == Outcome.IDEMPOTENT_REPLAY; }
    public boolean isIdempotencyConflict() { return outcome == Outcome.IDEMPOTENCY_CONFLICT; }
    public TransferRequestId getRequestId() { return requestId; }
    public TransactionId getTransactionId() { return transactionId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getSourceConsumerId() { return sourceConsumerId; }
    public String getDestinationConsumerId() { return destinationConsumerId; }
    public Currency getCurrency() { return currency; }
    public int getQuantity() { return quantity; }
    public String getReference() { return reference; }
    public Optional<Integer> getSourceBalanceBefore() { return Optional.ofNullable(sourceBalanceBefore); }
    public Optional<Integer> getSourceBalanceAfter() { return Optional.ofNullable(sourceBalanceAfter); }
    public Optional<Integer> getDestinationBalanceBefore() { return Optional.ofNullable(destinationBalanceBefore); }
    public Optional<Integer> getDestinationBalanceAfter() { return Optional.ofNullable(destinationBalanceAfter); }
    public Optional<TransferRejectionReason> getRejectionReason() { return Optional.ofNullable(rejectionReason); }
}
