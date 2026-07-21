package transaction;

import java.time.Instant;
import java.util.Objects;

public record TransactionRecord(
        TransactionId id,
        Instant occurredAt,
        TransactionType type,
        TransactionStatus status,
        TransactionDetails details
) {

    public TransactionRecord {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(details, "details must not be null");
        validateDetailsType(type, details);
    }

    private static void validateDetailsType(
            TransactionType type,
            TransactionDetails details
    ) {
        boolean valid = switch (type) {
            case MINT -> details instanceof MintTransactionDetails;
            case EXCHANGE -> details instanceof ExchangeTransactionDetails;
            case PURCHASE -> details instanceof PurchaseTransactionDetails;
            case TRANSFER -> details instanceof TransferTransactionDetails;
        };
        if (!valid) {
            throw new IllegalArgumentException(
                    "details do not match transaction type " + type
            );
        }
    }
}
