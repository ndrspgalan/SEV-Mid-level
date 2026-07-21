package banking.identity;

import java.util.Objects;

public record InstitutionalAccountId(String value) {
    public InstitutionalAccountId {
        Objects.requireNonNull(value, "institutional account id must not be null");
        value = value.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("institutional account id must not be blank");
    }
    public static InstitutionalAccountId compose(PersonName name, Profession profession, CensusPosition position, ReuseSequence reuse) {
        String base = name.initials() + "-" + profession.code() + "-" + position.formatted();
        return new InstitutionalAccountId(reuse.value() == 0 ? base : base + "-" + reuse.value());
    }
    @Override public String toString() { return value; }
}
