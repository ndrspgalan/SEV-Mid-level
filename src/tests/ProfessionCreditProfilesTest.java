package tests;

import coinProperties.Currency;
import consumableRegistry.ConsumableType;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;
import operationalControl.*;
import operationalControl.profile.ProfessionCreditProfile;
import operationalControl.profile.ValerianProfessionCreditProfiles;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

public final class ProfessionCreditProfilesTest {
    private static final Instant NOW = Instant.parse("1456-01-30T12:00:00Z");

    public static void main(String[] args) {
        testCanonicalMatrixHasNineteenDistinctProfessions();
        testGuerreroDeEbanoIsUnlimited();
        testMendigoRestrictions();
        testProfessionProfilesAreNotIdentical();
        testAllowedExchangeStillConsumesNumericLimit();
        System.out.println("ProfessionCreditProfilesTest: OK");
    }

    private static void testCanonicalMatrixHasNineteenDistinctProfessions() {
        assertEquals(19, ValerianProfessionCreditProfiles.all().size(), "canonical profession count");
        Set<String> names = new HashSet<>();
        for (ProfessionCreditProfile profile : ValerianProfessionCreditProfiles.all()) {
            assertTrue(names.add(profile.profession()), "profession must be unique");
        }
    }

    private static void testGuerreroDeEbanoIsUnlimited() {
        Fixture fixture = fixture("Kenan", "Guerrero de Ébano");
        OperationalAuthorization auth = fixture.controls.authorize(
                new OperationalControlRequest(fixture.consumer.getBankAccount(),
                        MonetaryOperationType.MINT, Currency.REAL_A5,
                        Integer.MAX_VALUE, NOW));
        assertTrue(auth.allowed(), "Guerrero de Ébano must be unlimited");
        assertTrue(auth.rejectionReason().isEmpty(), "unlimited profile has no rejection");
    }

    private static void testMendigoRestrictions() {
        Fixture fixture = fixture("Aldo", "Mendigo");
        var mint = fixture.controls.authorize(new OperationalControlRequest(
                fixture.consumer.getBankAccount(), MonetaryOperationType.MINT,
                Currency.BERYLARE, 1, NOW));
        assertFalse(mint.allowed(), "Mendigo cannot mint Berylares");
        assertEquals(OperationalControlRejectionReason.CURRENCY_NOT_ALLOWED_FOR_PROFESSION,
                mint.rejectionReason().orElseThrow(), "mint rejection reason");

        var exchange = fixture.controls.authorize(OperationalControlRequest.exchange(
                fixture.consumer.getBankAccount(), Currency.SUELDO,
                Currency.BERYLARE, 210, NOW));
        assertFalse(exchange.allowed(), "Mendigo cannot ascend to Berylare");
        assertEquals(OperationalControlRejectionReason.EXCHANGE_ROUTE_NOT_ALLOWED_FOR_PROFESSION,
                exchange.rejectionReason().orElseThrow(), "route rejection reason");

        var privatePurchase = fixture.controls.authorize(OperationalControlRequest.commercial(
                fixture.consumer.getBankAccount(), MonetaryOperationType.PURCHASE,
                Currency.SUELDO, ConsumableType.PRIVATE_USE, 1, NOW));
        assertFalse(privatePurchase.allowed(), "Mendigo cannot purchase private-use goods");
        assertEquals(OperationalControlRejectionReason.CONSUMABLE_TYPE_NOT_ALLOWED_FOR_PROFESSION,
                privatePurchase.rejectionReason().orElseThrow(), "product rejection reason");
    }

    private static void testProfessionProfilesAreNotIdentical() {
        Set<String> signatures = new HashSet<>();
        for (ProfessionCreditProfile p : ValerianProfessionCreditProfiles.all()) {
            String signature = p.monthlyEconomicCapacity() + ":" + p.dailyInteractionCount()
                    + ":" + p.maximumMintCurrency() + ":" + p.maximumExchangeCurrency()
                    + ":" + p.maximumPurchaseCurrency() + ":" + p.maximumTransferCurrency()
                    + ":" + p.maximumConsumableType() + ":" + p.unlimited();
            assertTrue(signatures.add(signature), "profiles must not be financially identical: " + p.profession());
        }
    }

    private static void testAllowedExchangeStillConsumesNumericLimit() {
        Fixture fixture = fixture("Bruno", "Jornalero");
        var auth = fixture.controls.authorize(OperationalControlRequest.exchange(
                fixture.consumer.getBankAccount(), Currency.VALERITA,
                Currency.SUELDO, 1_000, NOW));
        assertTrue(auth.allowed(), "ordinary exchange route should be allowed");
        fixture.controls.commit(auth);
        assertTrue(fixture.controls.usageFor(fixture.consumer.getBankAccount(), NOW).stream()
                .anyMatch(u -> u.operationType() == MonetaryOperationType.EXCHANGE
                        && u.accumulatedAmount() == 1_000),
                "allowed route must consume its numeric limit");
    }

    private static Fixture fixture(String name, String profession) {
        OperationalPolicyRegistry registry = new OperationalPolicyRegistry();
        ValerianProfessionCreditProfiles.install(registry, Instant.MIN);
        OperationalControlService controls = new OperationalControlService(registry, ZoneOffset.UTC);
        Consumer consumer = new ConsumerRegistry().register(name, profession);
        return new Fixture(controls, consumer);
    }

    private record Fixture(OperationalControlService controls, Consumer consumer) {}

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static void assertFalse(boolean condition, String message) { assertTrue(!condition, message); }
    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) throw new AssertionError(message + " expected=" + expected + " actual=" + actual);
    }
}
