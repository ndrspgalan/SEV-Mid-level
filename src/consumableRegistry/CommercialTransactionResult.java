package consumableRegistry;

import java.util.Objects;
import java.util.Optional;

public final class CommercialTransactionResult {

    private final boolean accepted;
    private final CommercialTransactionRejectionReason rejectionReason;

    private CommercialTransactionResult(
            boolean accepted,
            CommercialTransactionRejectionReason rejectionReason
    ) {
        this.accepted = accepted;
        this.rejectionReason = rejectionReason;
    }

    public static CommercialTransactionResult accepted() {
        return new CommercialTransactionResult(true, null);
    }

    public static CommercialTransactionResult rejected(
            CommercialTransactionRejectionReason rejectionReason
    ) {
        return new CommercialTransactionResult(
                false,
                Objects.requireNonNull(rejectionReason)
        );
    }

    public boolean isAccepted() {
        return accepted;
    }

    public Optional<CommercialTransactionRejectionReason> getRejectionReason() {
        return Optional.ofNullable(rejectionReason);
    }
}
