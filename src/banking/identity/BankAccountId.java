package banking.identity;

import java.util.Objects;
import java.util.UUID;

public record BankAccountId(UUID value) {
    public BankAccountId { Objects.requireNonNull(value, "value must not be null"); }
    public static BankAccountId random() { return new BankAccountId(UUID.randomUUID()); }
    public static BankAccountId parse(String value) { return new BankAccountId(UUID.fromString(value.trim())); }
    @Override public String toString() { return value.toString(); }
}
