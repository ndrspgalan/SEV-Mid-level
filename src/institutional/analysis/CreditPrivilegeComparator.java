package institutional.analysis;

import coinProperties.Currency;
import consumableRegistry.ConsumableType;
import institutional.snapshot.MobilityDirection;
import operationalControl.profile.ProfessionCreditProfile;

/**
 * Compares the hard Junior thresholds without introducing synthetic LOW/MEDIUM/HIGH labels.
 * A direction exists only when the target weakly dominates the source in every threshold
 * and strictly exceeds it in at least one. Crossed profiles remain non-directional.
 */
public final class CreditPrivilegeComparator {
    public MobilityDirection compare(ProfessionCreditProfile source, ProfessionCreditProfile target) {
        if (source.unlimited() && !target.unlimited()) return MobilityDirection.DOWNWARD;
        if (!source.unlimited() && target.unlimited()) return MobilityDirection.UPWARD;
        boolean targetAtLeast = atLeast(target, source);
        boolean sourceAtLeast = atLeast(source, target);
        if (targetAtLeast && !sourceAtLeast) return MobilityDirection.UPWARD;
        if (sourceAtLeast && !targetAtLeast) return MobilityDirection.DOWNWARD;
        return MobilityDirection.NON_DIRECTIONAL;
    }

    private boolean atLeast(ProfessionCreditProfile a, ProfessionCreditProfile b) {
        return a.monthlyEconomicCapacity() >= b.monthlyEconomicCapacity()
                && a.dailyInteractionCount() >= b.dailyInteractionCount()
                && rank(a.maximumMintCurrency()) >= rank(b.maximumMintCurrency())
                && rank(a.maximumExchangeCurrency()) >= rank(b.maximumExchangeCurrency())
                && rank(a.maximumPurchaseCurrency()) >= rank(b.maximumPurchaseCurrency())
                && rank(a.maximumTransferCurrency()) >= rank(b.maximumTransferCurrency())
                && a.maximumConsumableType().ordinal() >= b.maximumConsumableType().ordinal();
    }

    private int rank(Currency currency) { return currency.ordinal(); }
}
