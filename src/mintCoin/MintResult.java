package mintCoin;

import java.util.Objects;
import java.util.Optional;

public final class MintResult {

    private final boolean accepted;
    private final MintRejectionReason rejectionReason;

    private MintResult(boolean accepted, MintRejectionReason rejectionReason) {
        this.accepted = accepted;
        this.rejectionReason = rejectionReason;
    }

    public static MintResult accepted() {
        return new MintResult(true, null);
    }

    public static MintResult rejected(MintRejectionReason rejectionReason) {
        return new MintResult(false, Objects.requireNonNull(rejectionReason));
    }

    public boolean isAccepted() {
        return accepted;
    }

    public Optional<MintRejectionReason> getRejectionReason() {
        return Optional.ofNullable(rejectionReason);
    }
}
