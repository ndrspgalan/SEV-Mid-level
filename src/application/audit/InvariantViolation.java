package application.audit;

import java.util.Objects;

public record InvariantViolation(String code, String label, String context) {
    public InvariantViolation {
        code = requireText(code, "code");
        label = requireText(label, "label");
        context = Objects.requireNonNullElse(context, "").trim();
    }

    private static String requireText(String value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        String normalized = value.trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}
