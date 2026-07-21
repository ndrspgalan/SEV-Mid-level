package banking.lifecycle;

import java.util.Objects;
import java.util.UUID;

public record AccountLifecycleRequestId(UUID value) {
    public AccountLifecycleRequestId { Objects.requireNonNull(value, "value must not be null"); }
    public static AccountLifecycleRequestId generate() { return new AccountLifecycleRequestId(UUID.randomUUID()); }
    public static AccountLifecycleRequestId parse(String value) { return new AccountLifecycleRequestId(UUID.fromString(value.trim())); }
    @Override public String toString() { return value.toString(); }
}
