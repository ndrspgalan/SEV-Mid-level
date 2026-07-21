package consumableRegistry;

import java.util.Objects;

/**
 * Qualitative production factors that justify a catalog price without charging
 * the complete reusable infrastructure to every unit produced.
 */
public record ConsumableProductionProfile(
        ProductionEffort materialEffort,
        ProductionEffort activeWork,
        ProductionEffort transformationTime,
        ProductionEffort technicalDifficulty,
        ProductionEffort qualityControl,
        ProductionEffort preservationAndPackaging,
        ProductionEffort scarcity,
        ProductionEffort productionRisk,
        Scalability scalability,
        String rationale
) {
    public ConsumableProductionProfile {
        Objects.requireNonNull(materialEffort);
        Objects.requireNonNull(activeWork);
        Objects.requireNonNull(transformationTime);
        Objects.requireNonNull(technicalDifficulty);
        Objects.requireNonNull(qualityControl);
        Objects.requireNonNull(preservationAndPackaging);
        Objects.requireNonNull(scarcity);
        Objects.requireNonNull(productionRisk);
        Objects.requireNonNull(scalability);
        Objects.requireNonNull(rationale);
        rationale = rationale.trim();
        if (rationale.isEmpty()) throw new IllegalArgumentException("rationale must not be blank");
    }

    public enum ProductionEffort { VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH }
    public enum Scalability { VERY_HIGH, HIGH, MEDIUM, LOW, VERY_LOW }
}
