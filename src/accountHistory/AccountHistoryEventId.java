package accountHistory;

import java.util.Objects;
import java.util.UUID;

public record AccountHistoryEventId(UUID value) {
    public AccountHistoryEventId { Objects.requireNonNull(value); }
    public static AccountHistoryEventId generate() { return new AccountHistoryEventId(UUID.randomUUID()); }
    public static AccountHistoryEventId parse(String raw) { return new AccountHistoryEventId(UUID.fromString(Objects.requireNonNull(raw).trim())); }
    @Override public String toString() { return value.toString(); }
}
