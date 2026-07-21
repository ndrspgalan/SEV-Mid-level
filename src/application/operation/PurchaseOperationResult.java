package application.operation;

import coinProperties.Currency;
import consumableRegistry.CommercialTransactionRejectionReason;

import java.util.Objects;
import java.util.Optional;

public final class PurchaseOperationResult {

    public enum RejectionReason {
        BUYER_NOT_FOUND,
        SELLER_NOT_FOUND,
        CONSUMABLE_NOT_FOUND,
        TRANSACTION_POLICY_REJECTION
    }

    private final boolean accepted;
    private final String buyerName;
    private final String sellerName;
    private final String consumableName;
    private final Currency currency;
    private final int price;
    private final int buyerBalanceBefore;
    private final int buyerBalanceAfter;
    private final int sellerBalanceBefore;
    private final int sellerBalanceAfter;
    private final RejectionReason rejectionReason;
    private final CommercialTransactionRejectionReason policyRejectionReason;

    private PurchaseOperationResult(
            boolean accepted,
            String buyerName,
            String sellerName,
            String consumableName,
            Currency currency,
            int price,
            int buyerBalanceBefore,
            int buyerBalanceAfter,
            int sellerBalanceBefore,
            int sellerBalanceAfter,
            RejectionReason rejectionReason,
            CommercialTransactionRejectionReason policyRejectionReason
    ) {
        this.accepted = accepted;
        this.buyerName = buyerName;
        this.sellerName = sellerName;
        this.consumableName = consumableName;
        this.currency = currency;
        this.price = price;
        this.buyerBalanceBefore = buyerBalanceBefore;
        this.buyerBalanceAfter = buyerBalanceAfter;
        this.sellerBalanceBefore = sellerBalanceBefore;
        this.sellerBalanceAfter = sellerBalanceAfter;
        this.rejectionReason = rejectionReason;
        this.policyRejectionReason = policyRejectionReason;
    }

    public static PurchaseOperationResult rejected(RejectionReason reason) {
        return new PurchaseOperationResult(
                false, null, null, null, null,
                0, 0, 0, 0, 0,
                Objects.requireNonNull(reason), null
        );
    }

    public static PurchaseOperationResult policyRejected(
            CommercialTransactionRejectionReason reason
    ) {
        return new PurchaseOperationResult(
                false, null, null, null, null,
                0, 0, 0, 0, 0,
                RejectionReason.TRANSACTION_POLICY_REJECTION,
                Objects.requireNonNull(reason)
        );
    }

    public static PurchaseOperationResult accepted(
            String buyerName,
            String sellerName,
            String consumableName,
            Currency currency,
            int price,
            int buyerBalanceBefore,
            int buyerBalanceAfter,
            int sellerBalanceBefore,
            int sellerBalanceAfter
    ) {
        return new PurchaseOperationResult(
                true, buyerName, sellerName, consumableName, currency,
                price, buyerBalanceBefore, buyerBalanceAfter,
                sellerBalanceBefore, sellerBalanceAfter,
                null, null
        );
    }

    public boolean isAccepted() { return accepted; }
    public String getBuyerName() { return buyerName; }
    public String getSellerName() { return sellerName; }
    public String getConsumableName() { return consumableName; }
    public Currency getCurrency() { return currency; }
    public int getPrice() { return price; }
    public int getBuyerBalanceBefore() { return buyerBalanceBefore; }
    public int getBuyerBalanceAfter() { return buyerBalanceAfter; }
    public int getSellerBalanceBefore() { return sellerBalanceBefore; }
    public int getSellerBalanceAfter() { return sellerBalanceAfter; }
    public Optional<RejectionReason> getRejectionReason() { return Optional.ofNullable(rejectionReason); }
    public Optional<CommercialTransactionRejectionReason> getPolicyRejectionReason() { return Optional.ofNullable(policyRejectionReason); }
}
