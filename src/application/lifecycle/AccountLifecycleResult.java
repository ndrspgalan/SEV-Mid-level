package application.lifecycle;

import banking.lifecycle.AccountLifecycleRequestId;
import banking.lifecycle.AccountOperationalStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class AccountLifecycleResult {
    private final boolean completed;
    private final boolean idempotentReplay;
    private final AccountLifecycleRequestId requestId;
    private final Instant occurredAt;
    private final String bankAccountId;
    private final AccountOperationalStatus previousStatus;
    private final AccountOperationalStatus currentStatus;
    private final AccountLifecycleRejectionReason rejectionReason;

    private AccountLifecycleResult(boolean completed, boolean replay, AccountLifecycleRequestId requestId,
                                   Instant occurredAt, String bankAccountId, AccountOperationalStatus previousStatus,
                                   AccountOperationalStatus currentStatus, AccountLifecycleRejectionReason rejectionReason) {
        this.completed = completed; this.idempotentReplay = replay; this.requestId = Objects.requireNonNull(requestId);
        this.occurredAt = Objects.requireNonNull(occurredAt); this.bankAccountId = bankAccountId;
        this.previousStatus = previousStatus; this.currentStatus = currentStatus; this.rejectionReason = rejectionReason;
    }
    public static AccountLifecycleResult completed(AccountLifecycleRequestId id, Instant at, String accountId,
                                                    AccountOperationalStatus previous, AccountOperationalStatus current) {
        return new AccountLifecycleResult(true, false, id, at, accountId, previous, current, null);
    }
    public static AccountLifecycleResult rejected(AccountLifecycleRequestId id, Instant at, String accountId,
                                                   AccountOperationalStatus current, AccountLifecycleRejectionReason reason) {
        return new AccountLifecycleResult(false, false, id, at, accountId, current, current, Objects.requireNonNull(reason));
    }
    public AccountLifecycleResult asReplay() {
        return new AccountLifecycleResult(completed, true, requestId, occurredAt, bankAccountId, previousStatus, currentStatus, rejectionReason);
    }
    public boolean isCompleted() { return completed; }
    public boolean isIdempotentReplay() { return idempotentReplay; }
    public AccountLifecycleRequestId getRequestId() { return requestId; }
    public Instant getOccurredAt() { return occurredAt; }
    public Optional<String> getBankAccountId() { return Optional.ofNullable(bankAccountId); }
    public Optional<AccountOperationalStatus> getPreviousStatus() { return Optional.ofNullable(previousStatus); }
    public Optional<AccountOperationalStatus> getCurrentStatus() { return Optional.ofNullable(currentStatus); }
    public Optional<AccountLifecycleRejectionReason> getRejectionReason() { return Optional.ofNullable(rejectionReason); }
}
