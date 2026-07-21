package application.history;

import accountHistory.*;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

public final class AccountHistoryQueryService {
    private final AccountHistoryJournal journal;
    public AccountHistoryQueryService(AccountHistoryJournal journal) { this.journal = Objects.requireNonNull(journal); }

    public Optional<AccountHistoryEvent> findById(AccountHistoryEventId id) { return journal.findById(id); }

    public AccountHistoryPage search(AccountHistoryQuery query, AccountHistoryPageRequest pageRequest) {
        Objects.requireNonNull(query); Objects.requireNonNull(pageRequest);
        Comparator<AccountHistoryEvent> comparator = Comparator.comparing(AccountHistoryEvent::occurredAt)
                .thenComparing(e -> e.eventId().toString());
        if (query.sortDirection() == AccountHistorySortDirection.NEWEST_FIRST) comparator = comparator.reversed();
        List<AccountHistoryEvent> matches = journal.findAll().stream()
                .filter(matches(query))
                .sorted(comparator)
                .toList();
        long total = matches.size();
        int totalPages = total == 0 ? 0 : (int) ((total + pageRequest.pageSize() - 1) / pageRequest.pageSize());
        int from = Math.min(pageRequest.pageNumber() * pageRequest.pageSize(), matches.size());
        int to = Math.min(from + pageRequest.pageSize(), matches.size());
        return new AccountHistoryPage(matches.subList(from, to), pageRequest.pageNumber(), pageRequest.pageSize(), total, totalPages);
    }

    public List<AccountHistoryEvent> findAllForAccount(String bankAccountId) {
        return search(new AccountHistoryQuery(banking.identity.BankAccountId.parse(bankAccountId), null, null, null, null, null,
                AccountHistorySortDirection.NEWEST_FIRST), new AccountHistoryPageRequest(0, 100)).content();
    }

    private Predicate<AccountHistoryEvent> matches(AccountHistoryQuery query) {
        return event -> query.bankAccountIdFilter().map(id -> id.equals(event.bankAccountId())).orElse(true)
                && query.consumerIdFilter().map(id -> id.equals(event.consumerId())).orElse(true)
                && query.typeFilter().map(type -> type == event.type()).orElse(true)
                && query.statusFilter().map(status -> status == event.status()).orElse(true)
                && query.fromInclusiveFilter().map(from -> !event.occurredAt().isBefore(from)).orElse(true)
                && query.toExclusiveFilter().map(to -> event.occurredAt().isBefore(to)).orElse(true);
    }
}
