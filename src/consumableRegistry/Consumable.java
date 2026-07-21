package consumableRegistry;

import coinProperties.Currency;

import java.util.Objects;

/**
 * Canonical commercial good of SEV. There is deliberately no parallel Product
 * concept: every purchasable basic good is represented as a Consumable.
 *
 * <p>Pricing rule for future catalog extensions: the unit price is a normalized
 * economic estimate based on raw-material effort, direct active work, passive
 * transformation time, technical difficulty, quality control, preservation,
 * packaging, scarcity and production risk, corrected by production scalability.
 * Reusable infrastructure (mill, oven, workshop, instruments, etc.) must never
 * be charged in full to one unit; it is reflected only indirectly through the
 * production profile. This avoids absurd non-scalable prices while preserving
 * the real conceptual and material effort required to create the good.</p>
 *
 * <p>These catalog prices are normalized tracking values. They support stable
 * longitudinal comparison; they do not assert an ideal, immutable or exhaustive
 * model of market price formation. A real operational discrepancy is preserved
 * as evidence for Inspection rather than used to rewrite frozen history.</p>
 */
public final class Consumable {
    private final String consumableId;
    private final String name;
    private final ConsumableType type;
    private final ConsumableCategory category;
    private final Currency priceCurrency;
    private final int price;
    private final ConsumableProductionProfile productionProfile;

    public Consumable(String consumableId, String name, ConsumableType type,
                      ConsumableCategory category, Currency priceCurrency, int price,
                      ConsumableProductionProfile productionProfile) {
        this.consumableId = requireNonBlank(consumableId, "consumableId");
        this.name = requireNonBlank(name, "name");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.priceCurrency = Objects.requireNonNull(priceCurrency, "priceCurrency must not be null");
        if (price <= 0) throw new IllegalArgumentException("price must be greater than zero");
        this.price = price;
        this.productionProfile = Objects.requireNonNull(productionProfile, "productionProfile must not be null");
    }

    /** Compatibility constructor for older Junior fixtures. */
    public Consumable(String consumableId, String name, ConsumableType type,
                      Currency priceCurrency, int price) {
        this(consumableId, name, type, ConsumableCategory.FOOD, priceCurrency, price,
                new ConsumableProductionProfile(
                        ConsumableProductionProfile.ProductionEffort.LOW,
                        ConsumableProductionProfile.ProductionEffort.LOW,
                        ConsumableProductionProfile.ProductionEffort.LOW,
                        ConsumableProductionProfile.ProductionEffort.LOW,
                        ConsumableProductionProfile.ProductionEffort.LOW,
                        ConsumableProductionProfile.ProductionEffort.LOW,
                        ConsumableProductionProfile.ProductionEffort.LOW,
                        ConsumableProductionProfile.ProductionEffort.LOW,
                        ConsumableProductionProfile.Scalability.HIGH,
                        "Legacy catalog entry retained for backward compatibility."));
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) throw new IllegalArgumentException(fieldName + " must not be blank");
        return normalizedValue;
    }

    public String getConsumableId() { return consumableId; }
    public String getName() { return name; }
    public ConsumableType getType() { return type; }
    public ConsumableCategory getCategory() { return category; }
    public Currency getPriceCurrency() { return priceCurrency; }
    public int getPrice() { return price; }
    public ConsumableProductionProfile getProductionProfile() { return productionProfile; }
}
