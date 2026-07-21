package behavior.correlation.profile;

public record EconomicCorrelationClusterId(String value) {
    public EconomicCorrelationClusterId {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("value");
    }
}
