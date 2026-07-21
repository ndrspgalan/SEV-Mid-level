package application.view;

import transaction.TransactionId;
import transaction.TransactionStatus;
import transaction.TransactionType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record TransactionDetailView(
        TransactionId id,
        Instant occurredAt,
        TransactionType type,
        TransactionStatus status,
        String description,
        List<String> participantIds,
        Map<String, String> attributes
) {
    public TransactionDetailView {
        Objects.requireNonNull(id);
        Objects.requireNonNull(occurredAt);
        Objects.requireNonNull(type);
        Objects.requireNonNull(status);
        description = requireText(description, "description");
        participantIds = List.copyOf(Objects.requireNonNull(participantIds));
        attributes = Map.copyOf(Objects.requireNonNull(attributes));
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
