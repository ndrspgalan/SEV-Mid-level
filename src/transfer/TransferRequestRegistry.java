package transfer;

import java.util.Optional;

public interface TransferRequestRegistry {
    Optional<ProcessedTransferRequest> findById(TransferRequestId id);
    void register(ProcessedTransferRequest processedRequest);
}
