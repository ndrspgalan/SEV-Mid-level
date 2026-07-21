package application.lifecycle;

public enum AccountLifecycleRejectionReason {
    ACCOUNT_NOT_FOUND,
    ALREADY_BLOCKED,
    ACCOUNT_NOT_BLOCKED,
    ACCOUNT_ALREADY_CLOSED,
    ACCOUNT_PENDING_NEW_HOLDER,
    NON_ZERO_BALANCES,
    IDEMPOTENCY_CONFLICT
}
