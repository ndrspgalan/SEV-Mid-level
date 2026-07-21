package tests;

import application.ValerianEconomicSystem;
import application.ValerianEconomicSystemBootstrap;
import economicEvent.normalization.*;

public final class CompositeEconomicEventNormalizerTest {
    private CompositeEconomicEventNormalizerTest() {}

    public static void main(String[] args) {
        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();
        CompositeEconomicEventNormalizer normalizer = new CompositeEconomicEventNormalizer(
                new TransactionEconomicEventNormalizer(system.getConsumerRegistry()),
                new AccountHistoryEconomicEventNormalizer(),
                new OperationalDecisionEconomicEventNormalizer());

        Object source = system.getConsumerRegistry().getAccountHistoryJournal().findAll().get(0);
        check(normalizer.normalize(source).successful(), "account history source must be delegated");
        boolean unsupported = false;
        try { normalizer.normalize("unsupported"); } catch (IllegalArgumentException expected) { unsupported = true; }
        check(unsupported, "unsupported source must be rejected");
        System.out.println("CompositeEconomicEventNormalizerTest: PASSED");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
