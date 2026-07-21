package transaction;

import coinProperties.Currency;
import transfer.TransferRequestId;

import java.util.Objects;
import java.util.Optional;

public record TransferTransactionDetails(
        TransferRequestId requestId,
        String sourceConsumerId,
        String destinationConsumerId,
        Currency currency,
        int quantity,
        String reference,
        Optional<Integer> sourceBalanceBefore,
        Optional<Integer> sourceBalanceAfter,
        Optional<Integer> destinationBalanceBefore,
        Optional<Integer> destinationBalanceAfter,
        Optional<String> rejectionCode
) implements TransactionDetails {

    public TransferTransactionDetails {
        Objects.requireNonNull(requestId, "requestId must not be null");
        sourceConsumerId = requireText(sourceConsumerId, "sourceConsumerId");
        destinationConsumerId = requireText(destinationConsumerId, "destinationConsumerId");
        Objects.requireNonNull(currency, "currency must not be null");
        reference = requireText(reference, "reference");
        sourceBalanceBefore = Objects.requireNonNull(sourceBalanceBefore);
        sourceBalanceAfter = Objects.requireNonNull(sourceBalanceAfter);
        destinationBalanceBefore = Objects.requireNonNull(destinationBalanceBefore);
        destinationBalanceAfter = Objects.requireNonNull(destinationBalanceAfter);
        rejectionCode = Objects.requireNonNull(rejectionCode);
    }

    @Override
    public String summary() {
        if (rejectionCode.isPresent()) {
            return "Transferencia " + requestId + " rechazada: " + rejectionCode.get();
        }
        return "Transferencia de " + quantity + " " + currency
                + " desde " + sourceConsumerId + " hacia " + destinationConsumerId;
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
