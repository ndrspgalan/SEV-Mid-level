package transfer;

import java.util.Objects;
import java.util.UUID;

public record TransferRequestId(UUID value) {
    public TransferRequestId {
        Objects.requireNonNull(value, "value must not be null");
    }

    public static TransferRequestId generate() {
        return new TransferRequestId(UUID.randomUUID());
    }

    public static TransferRequestId parse(String value) {
        Objects.requireNonNull(value, "value must not be null");
        return new TransferRequestId(UUID.fromString(value.trim()));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
