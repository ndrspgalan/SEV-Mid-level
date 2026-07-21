package operationalControl.profile;

import coinProperties.Currency;
import consumableRegistry.ConsumableType;
import operationalControl.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Canonical, non-exhaustive profession-credit matrix for SEV Junior J7.2. */
public final class ValerianProfessionCreditProfiles {
    private static final long SUELDO = 1_000L;
    private static final long BERYLARE = 210_000L;
    private static final long REAL_A5 = 420_000L;

    private static final Map<Currency, Long> VALUE = Map.of(
            Currency.VALERITA, 1L,
            Currency.SUELDO, SUELDO,
            Currency.BERYLARE, BERYLARE,
            Currency.REAL_A5, REAL_A5
    );

    private static final List<ProfessionCreditProfile> PROFILES = List.of(
            new ProfessionCreditProfile("Guerrero de Ébano", 1, 1, Currency.REAL_A5, Currency.REAL_A5, Currency.REAL_A5, Currency.REAL_A5, ConsumableType.PRIVATE_USE, true),
            new ProfessionCreditProfile("Comerciante", 42_000_000L, 180, Currency.BERYLARE, Currency.REAL_A5, Currency.REAL_A5, Currency.REAL_A5, ConsumableType.PRIVATE_USE, false),
            new ProfessionCreditProfile("Cortesana", 4_600_000L, 54, Currency.SUELDO, Currency.BERYLARE, Currency.BERYLARE, Currency.BERYLARE, ConsumableType.PRIVATE_USE, false),
            new ProfessionCreditProfile("Mercenario", 9_800_000L, 32, Currency.BERYLARE, Currency.BERYLARE, Currency.REAL_A5, Currency.BERYLARE, ConsumableType.PRIVATE_USE, false),
            new ProfessionCreditProfile("Mendigo", 24_000L, 10, Currency.VALERITA, Currency.SUELDO, Currency.SUELDO, Currency.SUELDO, ConsumableType.SOCIAL_UTILITY, false),
            new ProfessionCreditProfile("Noble", 95_000_000L, 75, Currency.REAL_A5, Currency.REAL_A5, Currency.REAL_A5, Currency.REAL_A5, ConsumableType.PRIVATE_USE, false),
            new ProfessionCreditProfile("Soldado", 2_750_000L, 28, Currency.SUELDO, Currency.BERYLARE, Currency.BERYLARE, Currency.BERYLARE, ConsumableType.SOCIAL_UTILITY, false),
            new ProfessionCreditProfile("Herrero", 7_300_000L, 64, Currency.BERYLARE, Currency.BERYLARE, Currency.BERYLARE, Currency.BERYLARE, ConsumableType.PRIVATE_USE, false),
            new ProfessionCreditProfile("Carpintero", 4_900_000L, 52, Currency.SUELDO, Currency.BERYLARE, Currency.BERYLARE, Currency.BERYLARE, ConsumableType.SOCIAL_UTILITY, false),
            new ProfessionCreditProfile("Feriante", 13_500_000L, 260, Currency.SUELDO, Currency.BERYLARE, Currency.BERYLARE, Currency.BERYLARE, ConsumableType.PRIVATE_USE, false),
            new ProfessionCreditProfile("Maestro", 31_000_000L, 68, Currency.BERYLARE, Currency.REAL_A5, Currency.REAL_A5, Currency.REAL_A5, ConsumableType.PRIVATE_USE, false),
            new ProfessionCreditProfile("Jurista", 18_000_000L, 46, Currency.BERYLARE, Currency.REAL_A5, Currency.REAL_A5, Currency.REAL_A5, ConsumableType.PRIVATE_USE, false),
            new ProfessionCreditProfile("Cazador", 2_150_000L, 38, Currency.SUELDO, Currency.SUELDO, Currency.BERYLARE, Currency.SUELDO, ConsumableType.SOCIAL_UTILITY, false),
            new ProfessionCreditProfile("Marinero", 3_850_000L, 42, Currency.SUELDO, Currency.BERYLARE, Currency.BERYLARE, Currency.BERYLARE, ConsumableType.SOCIAL_UTILITY, false),
            new ProfessionCreditProfile("Curtidor", 5_600_000L, 58, Currency.SUELDO, Currency.BERYLARE, Currency.BERYLARE, Currency.BERYLARE, ConsumableType.PRIVATE_USE, false),
            new ProfessionCreditProfile("Modista", 4_250_000L, 72, Currency.SUELDO, Currency.BERYLARE, Currency.BERYLARE, Currency.SUELDO, ConsumableType.PRIVATE_USE, false),
            new ProfessionCreditProfile("Peluquero", 1_650_000L, 88, Currency.SUELDO, Currency.SUELDO, Currency.SUELDO, Currency.SUELDO, ConsumableType.SOCIAL_UTILITY, false),
            new ProfessionCreditProfile("Cantero", 6_450_000L, 36, Currency.BERYLARE, Currency.BERYLARE, Currency.BERYLARE, Currency.BERYLARE, ConsumableType.PRIVATE_USE, false),
            new ProfessionCreditProfile("Jornalero", 780_000L, 24, Currency.VALERITA, Currency.SUELDO, Currency.SUELDO, Currency.SUELDO, ConsumableType.SOCIAL_UTILITY, false)
    );

    private ValerianProfessionCreditProfiles() {}

    public static List<ProfessionCreditProfile> all() { return PROFILES; }

    public static void install(OperationalPolicyRegistry registry, Instant effectiveFrom) {
        for (ProfessionCreditProfile profile : PROFILES) installProfile(registry, profile, effectiveFrom);
    }

    private static void installProfile(OperationalPolicyRegistry registry,
            ProfessionCreditProfile profile, Instant from) {
        for (MonetaryOperationType operation : MonetaryOperationType.values()) {
            for (Currency currency : Currency.values()) {
                Currency ceiling = ceiling(profile, operation);
                if (rank(currency) > rank(ceiling)) {
                    registry.register(OperationalLimitPolicy.denied(PolicyScope.PROFESSION,
                            profile.profession(), operation, currency, null, null, from));
                } else if (profile.unlimited()) {
                    registry.register(OperationalLimitPolicy.unlimited(PolicyScope.PROFESSION,
                            profile.profession(), operation, currency, null, null, from));
                } else {
                    registerNumericPolicies(registry, profile, operation, currency, from);
                }
            }
        }

        installProductRules(registry, profile, from);
        installExchangeRoutes(registry, profile, from);
    }

    private static void registerNumericPolicies(OperationalPolicyRegistry registry,
            ProfessionCreditProfile profile, MonetaryOperationType operation,
            Currency currency, Instant from) {
        double factor = operationFactor(operation);
        long monthlyEquivalent = Math.max(1L,
                Math.round(profile.monthlyEconomicCapacity() * factor));
        long weeklyEquivalent = Math.max(1L, monthlyEquivalent * 27L / 100L);
        long dailyEquivalent = Math.max(1L, monthlyEquivalent * 6L / 100L);
        long perOperationEquivalent = Math.max(1L,
                dailyEquivalent * perOperationShare(operation) / 100L);

        long unit = VALUE.get(currency);
        int perOperation = safeInt(Math.max(1L, perOperationEquivalent / unit));
        long dailyAmount = Math.max(1L, dailyEquivalent / unit);
        long weeklyAmount = Math.max(1L, weeklyEquivalent / unit);
        long monthlyAmount = Math.max(1L, monthlyEquivalent / unit);
        int dailyCount = Math.max(1,
                (int)Math.round(profile.dailyInteractionCount() * countFactor(operation)));
        int weeklyCount = Math.max(dailyCount, dailyCount * 6);
        int monthlyCount = Math.max(weeklyCount, dailyCount * 24);

        registry.register(OperationalLimitPolicy.limited(PolicyScope.PROFESSION,
                profile.profession(), operation, currency, null, null,
                LimitWindow.DAILY, perOperation, dailyAmount, dailyCount, from));
        registry.register(OperationalLimitPolicy.limited(PolicyScope.PROFESSION,
                profile.profession(), operation, currency, null, null,
                LimitWindow.WEEKLY, null, weeklyAmount, weeklyCount, from));
        registry.register(OperationalLimitPolicy.limited(PolicyScope.PROFESSION,
                profile.profession(), operation, currency, null, null,
                LimitWindow.MONTHLY, null, monthlyAmount, monthlyCount, from));
    }

    private static void installProductRules(OperationalPolicyRegistry registry,
            ProfessionCreditProfile profile, Instant from) {
        for (MonetaryOperationType operation : List.of(MonetaryOperationType.PURCHASE,
                MonetaryOperationType.SALE)) {
            for (ConsumableType type : ConsumableType.values()) {
                if (type.ordinal() > profile.maximumConsumableType().ordinal()) {
                    for (Currency currency : Currency.values()) {
                        registry.register(OperationalLimitPolicy.denied(PolicyScope.PROFESSION,
                                profile.profession(), operation, currency, null, type, from));
                    }
                }
            }
        }
    }

    private static void installExchangeRoutes(OperationalPolicyRegistry registry,
            ProfessionCreditProfile profile, Instant from) {
        Currency[][] routes = {
                {Currency.VALERITA, Currency.SUELDO},
                {Currency.SUELDO, Currency.VALERITA},
                {Currency.SUELDO, Currency.BERYLARE},
                {Currency.BERYLARE, Currency.SUELDO},
                {Currency.BERYLARE, Currency.REAL_A5},
                {Currency.REAL_A5, Currency.BERYLARE}
        };
        for (Currency[] route : routes) {
            boolean allowed = profile.unlimited()
                    || (rank(route[0]) <= rank(profile.maximumExchangeCurrency())
                    && rank(route[1]) <= rank(profile.maximumExchangeCurrency()));
            if (!allowed) {
                registry.register(OperationalLimitPolicy.denied(PolicyScope.PROFESSION,
                        profile.profession(), MonetaryOperationType.EXCHANGE,
                        route[0], route[1], null, from));
            }
        }
    }

    private static Currency ceiling(ProfessionCreditProfile profile,
            MonetaryOperationType operation) {
        return switch (operation) {
            case MINT -> profile.maximumMintCurrency();
            case EXCHANGE -> profile.maximumExchangeCurrency();
            case PURCHASE, SALE -> profile.maximumPurchaseCurrency();
            case TRANSFER_SENT, TRANSFER_RECEIVED -> profile.maximumTransferCurrency();
        };
    }

    private static double operationFactor(MonetaryOperationType operation) {
        return switch (operation) {
            case MINT -> 0.18;
            case PURCHASE -> 0.55;
            case SALE -> 0.72;
            case EXCHANGE -> 0.40;
            case TRANSFER_SENT -> 0.48;
            case TRANSFER_RECEIVED -> 0.65;
        };
    }

    private static int perOperationShare(MonetaryOperationType operation) {
        return switch (operation) {
            case MINT -> 45;
            case PURCHASE -> 35;
            case SALE -> 50;
            case EXCHANGE -> 55;
            case TRANSFER_SENT -> 60;
            case TRANSFER_RECEIVED -> 75;
        };
    }

    private static double countFactor(MonetaryOperationType operation) {
        return switch (operation) {
            case MINT -> 0.08;
            case PURCHASE -> 0.50;
            case SALE -> 0.65;
            case EXCHANGE -> 0.12;
            case TRANSFER_SENT -> 0.22;
            case TRANSFER_RECEIVED -> 0.30;
        };
    }

    private static int rank(Currency currency) {
        return switch (currency) {
            case VALERITA -> 0;
            case SUELDO -> 1;
            case BERYLARE -> 2;
            case REAL_A5 -> 3;
        };
    }

    private static int safeInt(long value) {
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)value;
    }

    public static Map<String, ProfessionCreditProfile> byProfession() {
        Map<String, ProfessionCreditProfile> result = new LinkedHashMap<>();
        for (ProfessionCreditProfile p : PROFILES) result.put(p.profession(), p);
        return Map.copyOf(result);
    }
}
