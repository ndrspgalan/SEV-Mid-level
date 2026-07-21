package banking.identity;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record PersonName(String value) {
    public PersonName {
        Objects.requireNonNull(value, "name must not be null");
        value = value.trim().replaceAll("\\s+", " ");
        if (value.isEmpty()) throw new IllegalArgumentException("name must not be blank");
    }
    public String initials() {
        String[] parts = value.split("[\\s-]+");
        List<String> initials = new ArrayList<>();
        for (String part : parts) {
            if (!part.isBlank()) initials.add(stripAccents(part.substring(0, 1)).toUpperCase());
        }
        return String.join("", initials);
    }
    private static String stripAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
    @Override public String toString() { return value; }
}
