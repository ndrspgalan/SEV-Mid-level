package application.operation;

import application.view.TransactionDetailView;
import application.view.TransactionSummary;
import transaction.TransactionId;
import transaction.TransactionLedger;
import transaction.TransactionRecord;
import transaction.query.SortDirection;
import transaction.query.TransactionPage;
import transaction.query.TransactionQuery;

import java.util.*;
import java.util.stream.Stream;

public final class TransactionQueryService {

    private final TransactionLedger ledger;
    private final TransactionViewMapper mapper;

    public TransactionQueryService(TransactionLedger ledger) {
        this.ledger = Objects.requireNonNull(ledger);
        this.mapper = new TransactionViewMapper();
    }

    public Optional<TransactionDetailView> findById(TransactionId id) {
        Objects.requireNonNull(id, "id must not be null");
        return ledger.findById(id).map(mapper::toDetail);
    }

    public TransactionPage<TransactionSummary> search(TransactionQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        List<TransactionRecord> matchingRecords = filtered(query).toList();
        long totalElements = matchingRecords.size();
        int totalPages = totalPages(totalElements, query.pageRequest().pageSize());
        int offset = query.pageRequest().offset();

        if (offset >= matchingRecords.size()) {
            return new TransactionPage<>(
                    List.of(),
                    query.pageRequest().pageNumber(),
                    query.pageRequest().pageSize(),
                    totalElements,
                    totalPages
            );
        }

        int endExclusive = Math.min(
                offset + query.pageRequest().pageSize(),
                matchingRecords.size()
        );

        List<TransactionSummary> content = new ArrayList<>();
        for (TransactionRecord record : matchingRecords.subList(offset, endExclusive)) {
            content.add(mapper.toSummary(record));
        }

        return new TransactionPage<>(
                content,
                query.pageRequest().pageNumber(),
                query.pageRequest().pageSize(),
                totalElements,
                totalPages
        );
    }

    List<TransactionRecord> matchingRecords(TransactionQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return filtered(query).toList();
    }

    private Stream<TransactionRecord> filtered(TransactionQuery query) {
        Comparator<TransactionRecord> comparator = Comparator
                .comparing(TransactionRecord::occurredAt)
                .thenComparing(record -> record.id().toString());

        if (query.sortDirection() == SortDirection.NEWEST_FIRST) {
            comparator = comparator.reversed();
        }

        return ledger.findAll().stream()
                .filter(record -> query.type()
                        .map(type -> record.type() == type)
                        .orElse(true))
                .filter(record -> query.status()
                        .map(status -> record.status() == status)
                        .orElse(true))
                .filter(record -> query.participantId()
                        .map(participant -> mapper.hasParticipant(record, participant))
                        .orElse(true))
                .filter(record -> query.occurredFromInclusive()
                        .map(from -> !record.occurredAt().isBefore(from))
                        .orElse(true))
                .filter(record -> query.occurredToExclusive()
                        .map(to -> record.occurredAt().isBefore(to))
                        .orElse(true))
                .sorted(comparator);
    }

    private int totalPages(long totalElements, int pageSize) {
        if (totalElements == 0) {
            return 0;
        }
        return Math.toIntExact((totalElements + pageSize - 1) / pageSize);
    }
}
