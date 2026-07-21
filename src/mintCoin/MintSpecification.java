package mintCoin;

import coinProperties.Currency;
import coinProperties.Material;
import coinProperties.SealType;
import coinProperties.Weight;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class MintSpecification {

    private final Currency currency;
    private final Material material;
    private final SealType sealType;
    private final Set<Weight> allowedWeights;

    public MintSpecification(
            Currency currency,
            Material material,
            SealType sealType,
            Set<Weight> allowedWeights
    ) {
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.material = Objects.requireNonNull(material, "material must not be null");
        this.sealType = Objects.requireNonNull(sealType, "sealType must not be null");
        Objects.requireNonNull(allowedWeights, "allowedWeights must not be null");

        if (sealType.getCurrency() != currency) {
            throw new IllegalArgumentException(
                    "sealType must belong to the specified currency"
            );
        }

        if (allowedWeights.isEmpty()) {
            throw new IllegalArgumentException(
                    "allowedWeights must contain at least one weight"
            );
        }

        this.allowedWeights = Set.copyOf(allowedWeights);
    }

    public Currency getCurrency() {
        return currency;
    }

    public boolean allowsMaterial(Material material) {
        return this.material == Objects.requireNonNull(material, "material must not be null");
    }

    public boolean allowsWeight(Weight weight) {
        return this.allowedWeights.contains(Objects.requireNonNull(weight, "weight must not be null"));
    }

    public boolean allowsSeal(SealType sealType) {
        return this.sealType == Objects.requireNonNull(sealType, "sealType must not be null");
    }

    public static Set<Weight> allOfficialWeights() {
        return EnumSet.allOf(Weight.class);
    }
}
