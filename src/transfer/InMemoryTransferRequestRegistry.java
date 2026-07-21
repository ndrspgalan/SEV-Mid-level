package transfer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryTransferRequestRegistry implements TransferRequestRegistry {
    private final Map<TransferRequestId, ProcessedTransferRequest> requests =
            new LinkedHashMap<>();

    @Override
    public synchronized Optional<ProcessedTransferRequest> findById(TransferRequestId id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(requests.get(id));
    }

    @Override
    public synchronized void register(ProcessedTransferRequest processedRequest) {
        Objects.requireNonNull(processedRequest, "processedRequest must not be null");
        TransferRequestId id = processedRequest.request().requestId();
        if (requests.containsKey(id)) {
            throw new IllegalArgumentException("transfer request id already exists: " + id);
        }
        requests.put(id, processedRequest);
    }
}
