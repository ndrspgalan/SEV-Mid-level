package transaction;

import coinProperties.Currency;
import coinProperties.Material;
import coinProperties.SealType;
import coinProperties.Weight;

import java.util.Objects;
import java.util.Optional;

public record MintTransactionDetails(
        Optional<String> consumerId,
        Currency currency,
        Material material,
        Weight coinWeight,
        SealType sealType,
        int totalWeightInGrams,
        double copperRatio,
        double silverRatio,
        double goldRatio,
        Optional<Integer> coinQuantity,
        Optional<Integer> remainingGrams,
        Optional<String> rejectionCode
) implements TransactionDetails {

    public MintTransactionDetails {
        consumerId = optionalText(consumerId, "consumerId");
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(material, "material must not be null");
        Objects.requireNonNull(coinWeight, "coinWeight must not be null");
        Objects.requireNonNull(sealType, "sealType must not be null");
        coinQuantity = Objects.requireNonNull(coinQuantity);
        remainingGrams = Objects.requireNonNull(remainingGrams);
        rejectionCode = optionalText(rejectionCode, "rejectionCode");
    }

    /** Backward-compatible constructor for legacy system-level mint records. */
    public MintTransactionDetails(
            Currency currency,
            Material material,
            Weight coinWeight,
            SealType sealType,
            int totalWeightInGrams,
            double copperRatio,
            double silverRatio,
            double goldRatio,
            Optional<Integer> coinQuantity,
            Optional<Integer> remainingGrams,
            Optional<String> rejectionCode
    ) {
        this(Optional.empty(), currency, material, coinWeight, sealType, totalWeightInGrams,
                copperRatio, silverRatio, goldRatio, coinQuantity, remainingGrams, rejectionCode);
    }

    @Override
    public String summary() {
        if (rejectionCode.isPresent()) {
            return "Acuñación de " + currency + " rechazada: " + rejectionCode.get();
        }
        return "Acuñación de " + coinQuantity.orElseThrow() + " " + currency;
    }

    private static Optional<String> optionalText(Optional<String> value, String field) {
        Objects.requireNonNull(value, field + " optional must not be null");
        return value.map(text -> {
            Objects.requireNonNull(text, field + " must not be null");
            String normalized = text.trim();
            if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
            return normalized;
        });
    }
}
