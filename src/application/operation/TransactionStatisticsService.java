package application.operation;

import application.view.TransactionStatistics;
import transaction.TransactionRecord;
import transaction.TransactionStatus;
import transaction.TransactionType;
import transaction.query.TransactionQuery;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class TransactionStatisticsService {

    private final TransactionQueryService queryService;

    public TransactionStatisticsService(TransactionQueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService);
    }

    public TransactionStatistics calculate(TransactionQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        List<TransactionRecord> records = queryService.matchingRecords(query);

        Map<TransactionType, Long> byType = new EnumMap<>(TransactionType.class);
        for (TransactionType type : TransactionType.values()) {
            byType.put(type, 0L);
        }

        Map<TransactionStatus, Long> byStatus =
                new EnumMap<>(TransactionStatus.class);
        for (TransactionStatus status : TransactionStatus.values()) {
            byStatus.put(status, 0L);
        }

        for (TransactionRecord record : records) {
            byType.compute(record.type(), (ignored, count) -> count + 1);
            byStatus.compute(record.status(), (ignored, count) -> count + 1);
        }

        return new TransactionStatistics(records.size(), byType, byStatus);
    }
}
