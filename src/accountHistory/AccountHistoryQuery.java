package accountHistory;

import banking.identity.BankAccountId;
import banking.identity.ConsumerId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AccountHistoryQuery(BankAccountId bankAccountId, ConsumerId consumerId,
                                  AccountHistoryEventType type, AccountHistoryEventStatus status,
                                  Instant fromInclusive, Instant toExclusive,
                                  AccountHistorySortDirection sortDirection) {
    public AccountHistoryQuery {
        sortDirection = Objects.requireNonNullElse(sortDirection, AccountHistorySortDirection.NEWEST_FIRST);
        if (fromInclusive != null && toExclusive != null && !fromInclusive.isBefore(toExclusive)) {
            throw new IllegalArgumentException("fromInclusive must be before toExclusive");
        }
    }
    public static AccountHistoryQuery all() { return new AccountHistoryQuery(null, null, null, null, null, null, AccountHistorySortDirection.NEWEST_FIRST); }
    public Optional<BankAccountId> bankAccountIdFilter() { return Optional.ofNullable(bankAccountId); }
    public Optional<ConsumerId> consumerIdFilter() { return Optional.ofNullable(consumerId); }
    public Optional<AccountHistoryEventType> typeFilter() { return Optional.ofNullable(type); }
    public Optional<AccountHistoryEventStatus> statusFilter() { return Optional.ofNullable(status); }
    public Optional<Instant> fromInclusiveFilter() { return Optional.ofNullable(fromInclusive); }
    public Optional<Instant> toExclusiveFilter() { return Optional.ofNullable(toExclusive); }
}
