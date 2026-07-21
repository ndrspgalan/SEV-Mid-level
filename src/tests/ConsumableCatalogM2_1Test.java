package tests;

import application.ValerianEconomicSystem;
import application.ValerianEconomicSystemBootstrap;
import coinProperties.Currency;
import consumableRegistry.*;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ConsumableCatalogM2_1Test {
    public static void main(String[] args) {
        catalogIsCanonicalAndComplete();
        bootstrapRegistersCanonicalCatalog();
        expensiveFormulationsRemainBelowOneSueldo();
        System.out.println("ConsumableCatalogM2_1Test: OK");
    }

    private static void catalogIsCanonicalAndComplete() {
        var entries = ValerianBasicConsumableCatalog.entries();
        check(entries.size() == 11, "catalog must contain the eleven agreed consumables");
        Set<String> ids = new HashSet<>();
        Map<ConsumableCategory, Integer> byCategory = new EnumMap<>(ConsumableCategory.class);
        for (Consumable consumable : entries) {
            check(ids.add(consumable.getConsumableId()), "consumable ids must be unique");
            check(consumable.getType() == ConsumableType.BASIC_NECESSITY, "all initial goods are basic necessities");
            check(consumable.getPriceCurrency() == Currency.VALERITA, "initial catalog must be priced in Valeritas");
            check(consumable.getProductionProfile().rationale().length() > 20, "every price needs production rationale");
            byCategory.merge(consumable.getCategory(), 1, Integer::sum);
        }
        check(byCategory.get(ConsumableCategory.FOOD) == 4, "four foods expected");
        check(byCategory.get(ConsumableCategory.HEALING) == 3, "three healing consumables expected");
        check(byCategory.get(ConsumableCategory.STIMULANT) == 4, "four stimulants expected");
    }

    private static void bootstrapRegistersCanonicalCatalog() {
        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();
        for (Consumable expected : ValerianBasicConsumableCatalog.entries()) {
            Consumable actual = system.getConsumableRegistry().findById(expected.getConsumableId()).orElseThrow();
            check(actual.getName().equals(expected.getName()), "registered name mismatch");
            check(actual.getCategory() == expected.getCategory(), "registered category mismatch");
            check(actual.getPrice() == expected.getPrice(), "registered price mismatch");
        }
    }

    private static void expensiveFormulationsRemainBelowOneSueldo() {
        Consumable injection = find("STIM-001");
        Consumable lucidity = find("STIM-004");
        check(injection.getPrice() == 90, "injection price");
        check(lucidity.getPrice() == 120, "lucidity price");
        check(injection.getPrice() < 210 && lucidity.getPrice() < 210,
                "expensive basic consumables should still normally be expressed in Valeritas");
    }

    private static Consumable find(String id) {
        return ValerianBasicConsumableCatalog.entries().stream()
                .filter(value -> value.getConsumableId().equals(id)).findFirst().orElseThrow();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
