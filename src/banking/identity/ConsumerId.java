package banking.identity;

import java.util.Objects;
import java.util.UUID;

public record ConsumerId(UUID value) {
    public ConsumerId { Objects.requireNonNull(value, "value must not be null"); }
    public static ConsumerId random() { return new ConsumerId(UUID.randomUUID()); }
    public static ConsumerId parse(String value) { return new ConsumerId(UUID.fromString(require(value))); }
    private static String require(String value) { Objects.requireNonNull(value); return value.trim(); }
    @Override public String toString() { return value.toString(); }
}
