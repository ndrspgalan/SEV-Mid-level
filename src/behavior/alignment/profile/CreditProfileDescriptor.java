package behavior.alignment.profile;
import coinProperties.Currency;
import consumableRegistry.ConsumableType;
import operationalControl.profile.ProfessionCreditProfile;
import java.util.Objects;
/** Immutable institutional representation of a profession credit profile. */
public record CreditProfileDescriptor(String profession,long monthlyEconomicCapacity,int dailyInteractionCount,
 Currency maximumMintCurrency,Currency maximumExchangeCurrency,Currency maximumPurchaseCurrency,
 Currency maximumTransferCurrency,ConsumableType maximumConsumableType,boolean unlimited){
 public CreditProfileDescriptor{if(profession==null||profession.isBlank())throw new IllegalArgumentException("profession");Objects.requireNonNull(maximumMintCurrency);Objects.requireNonNull(maximumExchangeCurrency);Objects.requireNonNull(maximumPurchaseCurrency);Objects.requireNonNull(maximumTransferCurrency);Objects.requireNonNull(maximumConsumableType);}
 public static CreditProfileDescriptor from(ProfessionCreditProfile p){Objects.requireNonNull(p);return new CreditProfileDescriptor(p.profession(),p.monthlyEconomicCapacity(),p.dailyInteractionCount(),p.maximumMintCurrency(),p.maximumExchangeCurrency(),p.maximumPurchaseCurrency(),p.maximumTransferCurrency(),p.maximumConsumableType(),p.unlimited());}
}
