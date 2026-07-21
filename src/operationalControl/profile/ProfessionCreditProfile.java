package operationalControl.profile;

import coinProperties.Currency;
import consumableRegistry.ConsumableType;

public record ProfessionCreditProfile(
        String profession,
        long monthlyEconomicCapacity,
        int dailyInteractionCount,
        Currency maximumMintCurrency,
        Currency maximumExchangeCurrency,
        Currency maximumPurchaseCurrency,
        Currency maximumTransferCurrency,
        ConsumableType maximumConsumableType,
        boolean unlimited
) {
    public ProfessionCreditProfile {
        if (profession == null || profession.isBlank()) throw new IllegalArgumentException("profession must not be blank");
        if (!unlimited && monthlyEconomicCapacity <= 0) throw new IllegalArgumentException("monthly capacity must be positive");
        if (!unlimited && dailyInteractionCount <= 0) throw new IllegalArgumentException("daily count must be positive");
    }
}
