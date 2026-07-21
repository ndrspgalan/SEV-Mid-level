package behavior.correlation.profile;

import behavior.alignment.profile.StructuralAlignmentId;
import java.util.Objects;

public record EconomicCorrelationId(String value) {
    public EconomicCorrelationId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("value");
    }

    public static EconomicCorrelationId between(StructuralAlignmentId first, StructuralAlignmentId second) {
        Objects.requireNonNull(first); Objects.requireNonNull(second);
        String a = first.value(); String b = second.value();
        return new EconomicCorrelationId(a.compareTo(b) <= 0 ? "CORR|" + a + "|" + b : "CORR|" + b + "|" + a);
    }
}
