package mintCoin;

import coinProperties.Currency;
import coinProperties.Material;
import coinProperties.SealType;
import coinProperties.Weight;

public interface MintPolicy {

    MintResult mint(
            Currency currency,
            Material material,
            Weight weight,
            SealType sealType
    );
}
