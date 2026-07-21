package banking.identity;

import java.util.Objects;

public record Profession(String name, ProfessionCode code) {
    public Profession {
        Objects.requireNonNull(name, "profession name must not be null");
        Objects.requireNonNull(code, "profession code must not be null");
        name = name.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) throw new IllegalArgumentException("profession name must not be blank");
    }
    @Override public String toString() { return name; }
}
