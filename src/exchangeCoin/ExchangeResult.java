package exchangeCoin;

import java.util.Objects;
import java.util.Optional;

public final class ExchangeResult {

    private final boolean accepted;
    private final int targetQuantity;
    private final ExchangeRejectionReason rejectionReason;

    private ExchangeResult(
            boolean accepted,
            int targetQuantity,
            ExchangeRejectionReason rejectionReason
    ) {
        this.accepted = accepted;
        this.targetQuantity = targetQuantity;
        this.rejectionReason = rejectionReason;
    }

    public static ExchangeResult accepted(int targetQuantity) {
        if (targetQuantity <= 0) {
            throw new IllegalArgumentException("targetQuantity must be greater than zero");
        }
        return new ExchangeResult(true, targetQuantity, null);
    }

    public static ExchangeResult rejected(ExchangeRejectionReason rejectionReason) {
        return new ExchangeResult(false, 0, Objects.requireNonNull(rejectionReason));
    }

    public boolean isAccepted() {
        return accepted;
    }

    public int getTargetQuantity() {
        return targetQuantity;
    }

    public Optional<ExchangeRejectionReason> getRejectionReason() {
        return Optional.ofNullable(rejectionReason);
    }
}
