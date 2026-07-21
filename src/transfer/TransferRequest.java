package transfer;

import coinProperties.Currency;

import java.util.Objects;

public record TransferRequest(
        TransferRequestId requestId,
        String sourceConsumerId,
        String destinationConsumerId,
        Currency currency,
        int quantity,
        String reference
) {
    public TransferRequest {
        Objects.requireNonNull(requestId, "requestId must not be null");
        sourceConsumerId = requireText(sourceConsumerId, "sourceConsumerId");
        destinationConsumerId = requireText(destinationConsumerId, "destinationConsumerId");
        Objects.requireNonNull(currency, "currency must not be null");
        reference = requireText(reference, "reference");
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return normalized;
    }
}
