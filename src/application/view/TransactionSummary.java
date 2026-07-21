package application.view;

import transaction.TransactionId;
import transaction.TransactionStatus;
import transaction.TransactionType;

import java.time.Instant;
import java.util.Objects;

public record TransactionSummary(
        TransactionId id,
        Instant occurredAt,
        TransactionType type,
        TransactionStatus status,
        String description
) {
    public TransactionSummary {
        Objects.requireNonNull(id);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(type);
        Objects.requireNonNull(status);
        description = requireText(description, "description");
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
