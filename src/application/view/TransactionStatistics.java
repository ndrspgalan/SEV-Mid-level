package application.view;

import transaction.TransactionStatus;
import transaction.TransactionType;

import java.util.Map;
import java.util.Objects;

public record TransactionStatistics(
        long total,
        Map<TransactionType, Long> byType,
        Map<TransactionStatus, Long> byStatus
) {
    public TransactionStatistics {
        if (total < 0) {
            throw new IllegalArgumentException("total must not be negative");
        }
        byType = Map.copyOf(Objects.requireNonNull(byType));
        byStatus = Map.copyOf(Objects.requireNonNull(byStatus));
    }

    public long count(TransactionType type) {
        return byType.getOrDefault(Objects.requireNonNull(type), 0L);
    }

    public long count(TransactionStatus status) {
        return byStatus.getOrDefault(Objects.requireNonNull(status), 0L);
    }
}
