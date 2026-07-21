package transaction.query;

import transaction.TransactionStatus;
import transaction.TransactionType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record TransactionQuery(
        Optional<TransactionType> type,
        Optional<TransactionStatus> status,
        Optional<String> participantId,
        Optional<Instant> occurredFromInclusive,
        Optional<Instant> occurredToExclusive,
        SortDirection sortDirection,
        PageRequest pageRequest
) {

    public TransactionQuery {
        type = Objects.requireNonNull(type, "type must not be null");
        status = Objects.requireNonNull(status, "status must not be null");
        participantId = normalizeParticipant(participantId);
        occurredFromInclusive = Objects.requireNonNull(
                occurredFromInclusive,
                "occurredFromInclusive must not be null"
        );
        occurredToExclusive = Objects.requireNonNull(
                occurredToExclusive,
                "occurredToExclusive must not be null"
        );
        sortDirection = Objects.requireNonNull(
                sortDirection,
                "sortDirection must not be null"
        );
        pageRequest = Objects.requireNonNull(
                pageRequest,
                "pageRequest must not be null"
        );

        if (occurredFromInclusive.isPresent()
                && occurredToExclusive.isPresent()
                && !occurredFromInclusive.get().isBefore(occurredToExclusive.get())) {
            throw new IllegalArgumentException(
                    "occurredFromInclusive must be before occurredToExclusive"
            );
        }
    }

    public static TransactionQuery all(PageRequest pageRequest) {
        return new TransactionQuery(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                SortDirection.NEWEST_FIRST,
                pageRequest
        );
    }

    private static Optional<String> normalizeParticipant(
            Optional<String> participantId
    ) {
        Objects.requireNonNull(participantId, "participantId must not be null");
        return participantId.map(value -> {
            String normalized = Objects.requireNonNull(value).trim();
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException(
                        "participantId must not be blank"
                );
            }
            return normalized;
        });
    }
}
