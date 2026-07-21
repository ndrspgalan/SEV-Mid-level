package mintCoin;

import coinProperties.Currency;
import coinProperties.Material;
import coinProperties.SealType;
import coinProperties.Weight;

import java.util.Objects;

public final class ImplementedMintPolicy implements MintPolicy {

    private final MintSpecificationCatalog specificationCatalog;

    public ImplementedMintPolicy(MintSpecificationCatalog specificationCatalog) {
        this.specificationCatalog = Objects.requireNonNull(
                specificationCatalog,
                "specificationCatalog must not be null"
        );
    }

    @Override
    public MintResult mint(
            Currency currency,
            Material material,
            Weight weight,
            SealType sealType
    ) {
        Objects.requireNonNull(currency, "currency must not be null");
        Objects.requireNonNull(material, "material must not be null");
        Objects.requireNonNull(weight, "weight must not be null");
        Objects.requireNonNull(sealType, "sealType must not be null");

        MintSpecification specification = specificationCatalog
                .findByCurrency(currency)
                .orElse(null);

        if (specification == null) {
            return MintResult.rejected(MintRejectionReason.SPECIFICATION_NOT_FOUND);
        }

        if (!specification.allowsMaterial(material)) {
            return MintResult.rejected(MintRejectionReason.MATERIAL_NOT_ALLOWED);
        }

        if (!specification.allowsWeight(weight)) {
            return MintResult.rejected(MintRejectionReason.WEIGHT_NOT_ALLOWED);
        }

        if (!specification.allowsSeal(sealType)) {
            return MintResult.rejected(MintRejectionReason.SEAL_NOT_ALLOWED);
        }

        return MintResult.accepted();
    }
}
