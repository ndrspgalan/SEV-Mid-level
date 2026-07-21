package behavior.correlation.analysis;

import behavior.alignment.profile.CreditProfileDescriptor;
import behavior.correlation.profile.InstitutionalProfileRelation;
import coinProperties.Currency;
import java.util.*;

/** Derives profile ordering directly from institutional capabilities, without scores or thresholds. */
public final class InstitutionalProfileTopology {
    private final Map<String, CreditProfileDescriptor> profiles;

    public InstitutionalProfileTopology(Collection<CreditProfileDescriptor> descriptors) {
        Objects.requireNonNull(descriptors);
        LinkedHashMap<String, CreditProfileDescriptor> copy = new LinkedHashMap<>();
        descriptors.stream().sorted(Comparator.comparing(CreditProfileDescriptor::profession)).forEach(p -> {
            if (copy.put(p.profession(), p) != null) throw new IllegalArgumentException("duplicate profile " + p.profession());
        });
        profiles = Collections.unmodifiableMap(copy);
    }

    public InstitutionalProfileRelation relation(String fromProfession, String toProfession) {
        CreditProfileDescriptor from = require(fromProfession);
        CreditProfileDescriptor to = require(toProfession);
        if (sameCapabilities(from, to)) return InstitutionalProfileRelation.EQUIVALENT;
        boolean toAtLeastFrom = atLeast(to, from);
        boolean fromAtLeastTo = atLeast(from, to);
        if (toAtLeastFrom && !fromAtLeastTo) return InstitutionalProfileRelation.MORE_EXPANSIVE;
        if (fromAtLeastTo && !toAtLeastFrom) return InstitutionalProfileRelation.MORE_RESTRICTIVE;
        return InstitutionalProfileRelation.CROSS_PROFILE;
    }

    public CreditProfileDescriptor require(String profession) {
        CreditProfileDescriptor profile = profiles.get(profession);
        if (profile == null) throw new IllegalArgumentException("unknown institutional profile: " + profession);
        return profile;
    }

    public Map<String, CreditProfileDescriptor> profiles() { return profiles; }

    private static boolean sameCapabilities(CreditProfileDescriptor a, CreditProfileDescriptor b) {
        return a.unlimited() == b.unlimited()
                && a.monthlyEconomicCapacity() == b.monthlyEconomicCapacity()
                && a.dailyInteractionCount() == b.dailyInteractionCount()
                && a.maximumMintCurrency() == b.maximumMintCurrency()
                && a.maximumExchangeCurrency() == b.maximumExchangeCurrency()
                && a.maximumPurchaseCurrency() == b.maximumPurchaseCurrency()
                && a.maximumTransferCurrency() == b.maximumTransferCurrency()
                && a.maximumConsumableType() == b.maximumConsumableType();
    }

    private static boolean atLeast(CreditProfileDescriptor candidate, CreditProfileDescriptor reference) {
        if (candidate.unlimited()) return true;
        if (reference.unlimited()) return false;
        return candidate.monthlyEconomicCapacity() >= reference.monthlyEconomicCapacity()
                && candidate.dailyInteractionCount() >= reference.dailyInteractionCount()
                && rank(candidate.maximumMintCurrency()) >= rank(reference.maximumMintCurrency())
                && rank(candidate.maximumExchangeCurrency()) >= rank(reference.maximumExchangeCurrency())
                && rank(candidate.maximumPurchaseCurrency()) >= rank(reference.maximumPurchaseCurrency())
                && rank(candidate.maximumTransferCurrency()) >= rank(reference.maximumTransferCurrency())
                && candidate.maximumConsumableType().ordinal() >= reference.maximumConsumableType().ordinal();
    }

    private static int rank(Currency currency) {
        return switch (currency) {
            case VALERITA -> 0;
            case SUELDO -> 1;
            case BERYLARE -> 2;
            case REAL_A5 -> 3;
        };
    }
}
