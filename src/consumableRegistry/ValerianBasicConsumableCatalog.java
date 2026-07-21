package consumableRegistry;

import coinProperties.Currency;
import consumableRegistry.ConsumableProductionProfile.ProductionEffort;
import consumableRegistry.ConsumableProductionProfile.Scalability;

import static consumableRegistry.ConsumableProductionProfile.ProductionEffort.*;

import java.util.List;

/** Initial canonical catalog: all entries are basic necessities priced in Valeritas. */
public final class ValerianBasicConsumableCatalog {
    private ValerianBasicConsumableCatalog() {}

    public static List<Consumable> entries() {
        return List.of(
                item("FOOD-001", "Pan", ConsumableCategory.FOOD, 3,
                        profile(LOW, LOW, MEDIUM, LOW, LOW, LOW, LOW, LOW, Scalability.VERY_HIGH,
                                "Common grain preparation; baking infrastructure is highly reusable and production scales well.")),
                item("FOOD-002", "Cecina", ConsumableCategory.FOOD, 12,
                        profile(MEDIUM, MEDIUM, HIGH, MEDIUM, MEDIUM, MEDIUM, MEDIUM, LOW, Scalability.MEDIUM,
                                "Requires animal raw material, salting, controlled drying and preservation time.")),
                item("FOOD-003", "Frutos Secos", ConsumableCategory.FOOD, 8,
                        profile(MEDIUM, LOW, MEDIUM, LOW, MEDIUM, MEDIUM, MEDIUM, LOW, Scalability.HIGH,
                                "Seasonal gathering or cultivation, selection, drying and durable packaging.")),
                item("FOOD-004", "Bizcocho", ConsumableCategory.FOOD, 7,
                        profile(LOW, MEDIUM, LOW, MEDIUM, LOW, LOW, LOW, LOW, Scalability.HIGH,
                                "Uses several common ingredients and more active preparation than bread, but remains batch-scalable.")),
                item("HEAL-001", "Emplasto de Milenrama", ConsumableCategory.HEALING, 10,
                        profile(LOW, MEDIUM, MEDIUM, MEDIUM, MEDIUM, MEDIUM, LOW, LOW, Scalability.MEDIUM,
                                "Selective plant preparation, cleaning and hygienic assembly justify a moderate medicinal premium.")),
                item("HEAL-002", "Parche de Llantén", ConsumableCategory.HEALING, 16,
                        profile(LOW, HIGH, MEDIUM, MEDIUM, HIGH, HIGH, LOW, LOW, Scalability.MEDIUM,
                                "Pressed plant material must be prepared, fixed to tissue and preserved as a reliable ready-to-use patch.")),
                item("HEAL-003", "Apósito de Musgo de Turbera", ConsumableCategory.HEALING, 24,
                        profile(MEDIUM, HIGH, MEDIUM, HIGH, HIGH, HIGH, MEDIUM, MEDIUM, Scalability.LOW,
                                "Requires suitable bog moss, cleaning, impregnation, hygienic control and structured fastening.")),
                item("STIM-001", "Inyección Estimulante", ConsumableCategory.STIMULANT, 90,
                        profile(HIGH, HIGH, MEDIUM, VERY_HIGH, VERY_HIGH, VERY_HIGH, HIGH, VERY_HIGH, Scalability.LOW,
                                "Concentrated formulation, precise dosage, sterility, injector manufacture and production risk dominate its cost.")),
                item("STIM-002", "Corteza de Sauce", ConsumableCategory.STIMULANT, 6,
                        profile(LOW, LOW, MEDIUM, LOW, MEDIUM, LOW, LOW, LOW, Scalability.HIGH,
                                "Accessible botanical material requiring correct harvesting, drying and portioning.")),
                item("STIM-003", "Hidromiel", ConsumableCategory.STIMULANT, 14,
                        profile(MEDIUM, MEDIUM, HIGH, MEDIUM, MEDIUM, MEDIUM, MEDIUM, MEDIUM, Scalability.HIGH,
                                "Honey and fermentation vessels are valuable; maturation is long but mostly passive and batch-scalable.")),
                item("STIM-004", "Esencia de Lucidez", ConsumableCategory.STIMULANT, 120,
                        profile(HIGH, HIGH, HIGH, VERY_HIGH, VERY_HIGH, VERY_HIGH, VERY_HIGH, HIGH, Scalability.VERY_LOW,
                                "Refined cognitive formulation requiring rare inputs, purity control, exact dosing and specialized containment."))
        );
    }

    public static void registerInto(ConsumableRegistry registry) {
        entries().forEach(registry::register);
    }

    private static Consumable item(String id, String name, ConsumableCategory category, int price,
                                   ConsumableProductionProfile profile) {
        return new Consumable(id, name, ConsumableType.BASIC_NECESSITY,
                category, Currency.VALERITA, price, profile);
    }

    private static ConsumableProductionProfile profile(
            ProductionEffort material, ProductionEffort work, ProductionEffort time,
            ProductionEffort technique, ProductionEffort quality, ProductionEffort packaging,
            ProductionEffort scarcity, ProductionEffort risk, Scalability scalability, String rationale) {
        return new ConsumableProductionProfile(material, work, time, technique, quality,
                packaging, scarcity, risk, scalability, rationale);
    }
}
