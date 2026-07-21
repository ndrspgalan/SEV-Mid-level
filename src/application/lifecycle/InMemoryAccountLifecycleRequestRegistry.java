package application.lifecycle;

import banking.lifecycle.AccountLifecycleRequestId;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryAccountLifecycleRequestRegistry implements AccountLifecycleRequestRegistry {
    private final Map<AccountLifecycleRequestId, ProcessedAccountLifecycleRequest> values = new LinkedHashMap<>();
    @Override public synchronized Optional<ProcessedAccountLifecycleRequest> find(AccountLifecycleRequestId id) {
        return Optional.ofNullable(values.get(Objects.requireNonNull(id)));
    }
    @Override public synchronized void register(ProcessedAccountLifecycleRequest processed) {
        Objects.requireNonNull(processed); ProcessedAccountLifecycleRequest previous = values.putIfAbsent(processed.request().requestId(), processed);
        if (previous != null) throw new IllegalStateException("lifecycle request already registered");
    }
}
