package application.lifecycle;

import banking.lifecycle.AccountLifecycleRequestId;

import java.util.Optional;

public interface AccountLifecycleRequestRegistry {
    Optional<ProcessedAccountLifecycleRequest> find(AccountLifecycleRequestId id);
    void register(ProcessedAccountLifecycleRequest processed);
}
