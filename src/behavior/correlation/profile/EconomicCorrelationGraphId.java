package behavior.correlation.profile;

import behavior.temporal.SeasonPeriod;
import java.util.Objects;

public record EconomicCorrelationGraphId(String value) {
    public EconomicCorrelationGraphId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("value");
    }
    public static EconomicCorrelationGraphId from(SeasonPeriod period) {
        return new EconomicCorrelationGraphId("ECONOMIC-CORRELATION-GRAPH|" + Objects.requireNonNull(period).label());
    }
}
