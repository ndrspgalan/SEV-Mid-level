package application.operation;

import coinProperties.Currency;
import mintCoin.MintRejectionReason;

import java.util.Objects;
import java.util.Optional;

public final class MintOperationResult {

    public enum RejectionReason {
        INVALID_MATERIAL_COMPOSITION,
        INSUFFICIENT_METAL,
        MINT_POLICY_REJECTION,
        CONSUMER_NOT_FOUND
    }

    private final boolean accepted;
    private final Currency currency;
    private final int coinQuantity;
    private final int remainingGrams;
    private final RejectionReason rejectionReason;
    private final MintRejectionReason policyRejectionReason;

    private MintOperationResult(
            boolean accepted,
            Currency currency,
            int coinQuantity,
            int remainingGrams,
            RejectionReason rejectionReason,
            MintRejectionReason policyRejectionReason
    ) {
        this.accepted = accepted;
        this.currency = currency;
        this.coinQuantity = coinQuantity;
        this.remainingGrams = remainingGrams;
        this.rejectionReason = rejectionReason;
        this.policyRejectionReason = policyRejectionReason;
    }

    public static MintOperationResult accepted(
            Currency currency,
            int coinQuantity,
            int remainingGrams
    ) {
        return new MintOperationResult(
                true,
                Objects.requireNonNull(currency),
                coinQuantity,
                remainingGrams,
                null,
                null
        );
    }

    public static MintOperationResult rejected(RejectionReason reason) {
        return new MintOperationResult(
                false,
                null,
                0,
                0,
                Objects.requireNonNull(reason),
                null
        );
    }

    public static MintOperationResult policyRejected(
            MintRejectionReason policyReason
    ) {
        return new MintOperationResult(
                false,
                null,
                0,
                0,
                RejectionReason.MINT_POLICY_REJECTION,
                Objects.requireNonNull(policyReason)
        );
    }

    public boolean isAccepted() {
        return accepted;
    }

    public Currency getCurrency() {
        return currency;
    }

    public int getCoinQuantity() {
        return coinQuantity;
    }

    public int getRemainingGrams() {
        return remainingGrams;
    }

    public Optional<RejectionReason> getRejectionReason() {
        return Optional.ofNullable(rejectionReason);
    }

    public Optional<MintRejectionReason> getPolicyRejectionReason() {
        return Optional.ofNullable(policyRejectionReason);
    }
}
