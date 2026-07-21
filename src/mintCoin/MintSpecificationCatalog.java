package mintCoin;

import coinProperties.Currency;
import coinProperties.Material;
import coinProperties.SealType;

import java.util.*;

public final class MintSpecificationCatalog {

    private final Map<Currency, MintSpecification> specifications;

    public MintSpecificationCatalog(Collection<MintSpecification> specifications) {
        Objects.requireNonNull(specifications, "specifications must not be null");

        EnumMap<Currency, MintSpecification> indexedSpecifications =
                new EnumMap<>(Currency.class);

        for (MintSpecification specification : specifications) {
            Objects.requireNonNull(
                    specification,
                    "specification must not be null"
            );

            MintSpecification previous = indexedSpecifications.putIfAbsent(
                    specification.getCurrency(),
                    specification
            );

            if (previous != null) {
                throw new IllegalArgumentException(
                        "Duplicate mint specification for currency: "
                                + specification.getCurrency()
                );
            }
        }

        this.specifications = Map.copyOf(indexedSpecifications);
    }

    public Optional<MintSpecification> findByCurrency(Currency currency) {
        Objects.requireNonNull(currency, "currency must not be null");
        return Optional.ofNullable(specifications.get(currency));
    }

    public static MintSpecificationCatalog valerianStandard() {
        return new MintSpecificationCatalog(
                List.of(
                        new MintSpecification(
                                Currency.VALERITA,
                                Material.COPPER,
                                SealType.V,
                                MintSpecification.allOfficialWeights()
                        ),
                        new MintSpecification(
                                Currency.SUELDO,
                                Material.SILVER_COPPER_ALLOY,
                                SealType.S,
                                MintSpecification.allOfficialWeights()
                        ),
                        new MintSpecification(
                                Currency.BERYLARE,
                                Material.SILVER,
                                SealType.B,
                                MintSpecification.allOfficialWeights()
                        ),
                        new MintSpecification(
                                Currency.REAL_A5,
                                Material.GOLD,
                                SealType.A5,
                                MintSpecification.allOfficialWeights()
                        )
                )
        );
    }
}
