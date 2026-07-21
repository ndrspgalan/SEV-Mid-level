package application.lifecycle;

import banking.lifecycle.AccountLifecycleRequest;

import java.util.Objects;

public record ProcessedAccountLifecycleRequest(AccountLifecycleRequest request, AccountLifecycleResult result) {
    public ProcessedAccountLifecycleRequest { Objects.requireNonNull(request); Objects.requireNonNull(result); }
}
