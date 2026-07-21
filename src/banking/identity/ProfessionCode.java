package banking.identity;

import java.util.Locale;
import java.util.Objects;

public record ProfessionCode(String value) {
    public ProfessionCode {
        Objects.requireNonNull(value, "profession code must not be null");
        value = value.trim();
        if (!value.matches("[A-Z][A-Za-z]{0,7}")) throw new IllegalArgumentException("invalid profession code: " + value);
    }
    public static ProfessionCode of(String value) {
        String normalized = value.trim();
        return new ProfessionCode(normalized.substring(0,1).toUpperCase(Locale.ROOT) + normalized.substring(1));
    }
    @Override public String toString() { return value; }
}
