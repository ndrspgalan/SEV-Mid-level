package transaction;

import coinProperties.Currency;
import consumableRegistry.ConsumableCategory;

import java.util.Objects;
import java.util.Optional;

public record PurchaseTransactionDetails(
        String buyerId,
        String sellerId,
        String consumableId,
        Optional<String> consumableName,
        Optional<ConsumableCategory> consumableCategory,
        Optional<Integer> quantity,
        Optional<Currency> currency,
        Optional<Integer> unitPrice,
        Optional<Integer> price,
        Optional<Integer> buyerBalanceBefore,
        Optional<Integer> buyerBalanceAfter,
        Optional<Integer> sellerBalanceBefore,
        Optional<Integer> sellerBalanceAfter,
        Optional<String> rejectionCode
) implements TransactionDetails {

    public PurchaseTransactionDetails {
        buyerId = requireText(buyerId, "buyerId");
        sellerId = requireText(sellerId, "sellerId");
        consumableId = requireText(consumableId, "consumableId");
        consumableName = Objects.requireNonNull(consumableName);
        consumableCategory = Objects.requireNonNull(consumableCategory);
        quantity = Objects.requireNonNull(quantity);
        currency = Objects.requireNonNull(currency);
        unitPrice = Objects.requireNonNull(unitPrice);
        price = Objects.requireNonNull(price);
        buyerBalanceBefore = Objects.requireNonNull(buyerBalanceBefore);
        buyerBalanceAfter = Objects.requireNonNull(buyerBalanceAfter);
        sellerBalanceBefore = Objects.requireNonNull(sellerBalanceBefore);
        sellerBalanceAfter = Objects.requireNonNull(sellerBalanceAfter);
        rejectionCode = Objects.requireNonNull(rejectionCode);
        consumableName.ifPresent(value -> requireText(value, "consumableName"));
        quantity.ifPresent(value -> { if (value <= 0) throw new IllegalArgumentException("quantity must be positive"); });
        unitPrice.ifPresent(value -> { if (value <= 0) throw new IllegalArgumentException("unitPrice must be positive"); });
    }

    /** Backward-compatible constructor for frozen M1 fixtures. */
    public PurchaseTransactionDetails(String buyerId, String sellerId, String consumableId,
            Optional<Currency> currency, Optional<Integer> price,
            Optional<Integer> buyerBalanceBefore, Optional<Integer> buyerBalanceAfter,
            Optional<Integer> sellerBalanceBefore, Optional<Integer> sellerBalanceAfter,
            Optional<String> rejectionCode) {
        this(buyerId, sellerId, consumableId, Optional.empty(), Optional.empty(),
                price.isPresent() ? Optional.of(1) : Optional.empty(), currency, price, price,
                buyerBalanceBefore, buyerBalanceAfter, sellerBalanceBefore, sellerBalanceAfter, rejectionCode);
    }

    @Override
    public String summary() {
        if (rejectionCode.isPresent()) return "Compra de " + consumableId + " rechazada: " + rejectionCode.get();
        return "Compra de " + quantity.orElse(1) + " x " + consumableName.orElse(consumableId)
                + " por " + price.orElseThrow() + " " + currency.orElseThrow();
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
