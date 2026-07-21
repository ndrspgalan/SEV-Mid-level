package banking.lifecycle;

import java.util.Objects;
import java.util.Optional;

public record AccountLifecycleRequest(AccountLifecycleRequestId requestId, String accountOrConsumerId,
                                      AccountLifecycleAction action, AccountClosureReason closureReason,
                                      String reference) {
    public AccountLifecycleRequest {
        Objects.requireNonNull(requestId, "requestId must not be null");
        accountOrConsumerId = requireText(accountOrConsumerId, "accountOrConsumerId");
        Objects.requireNonNull(action, "action must not be null");
        reference = requireText(reference, "reference");
        if (action == AccountLifecycleAction.CLOSE && closureReason == null) {
            throw new IllegalArgumentException("close action requires closure reason");
        }
        if (action != AccountLifecycleAction.CLOSE && closureReason != null) {
            throw new IllegalArgumentException("closure reason is only valid for close action");
        }
    }
    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field + " must not be null").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
    public Optional<AccountClosureReason> closureReasonOptional() { return Optional.ofNullable(closureReason); }
}
