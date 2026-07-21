package tests;

import application.ValerianEconomicSystem;
import application.ValerianEconomicSystemBootstrap;
import application.analytics.projection.EconomicEventProjectionResult;

public final class EconomicEventProjectionServiceTest {
    private EconomicEventProjectionServiceTest() {}

    public static void main(String[] args) {
        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();
        EconomicEventProjectionResult first = system.getEconomicEventProjectionService().projectAll();
        check(first.inspected() >= 2, "bootstrap account history must be inspected");
        check(first.created() > 0, "first projection must create events");
        check(first.failures().isEmpty(), "bootstrap projection must not fail");
        long count = system.getEconomicEventRepository().count();

        EconomicEventProjectionResult second = system.getEconomicEventProjectionService().projectAll();
        check(second.created() == 0, "second projection must be idempotent");
        check(second.alreadyPresent() == first.created(), "all projected events must already exist");
        check(system.getEconomicEventRepository().count() == count, "repository size must remain stable");

        System.out.println("EconomicEventProjectionServiceTest: PASSED");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
