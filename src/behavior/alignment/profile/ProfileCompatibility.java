package behavior.alignment.profile;
import java.util.*;
/** Complete, explainable comparison against one institutional profile. */
public record ProfileCompatibility(CreditProfileDescriptor profile,Map<CreditProfileDimension,DimensionCompatibility> dimensions,List<String> explanations){
 public ProfileCompatibility{Objects.requireNonNull(profile);EnumMap<CreditProfileDimension,DimensionCompatibility> copy=new EnumMap<>(CreditProfileDimension.class);copy.putAll(Objects.requireNonNull(dimensions));for(CreditProfileDimension d:CreditProfileDimension.values())copy.putIfAbsent(d,DimensionCompatibility.UNOBSERVED);dimensions=Collections.unmodifiableMap(copy);explanations=List.copyOf(Objects.requireNonNull(explanations));}
 public boolean compatible(){return dimensions.values().stream().noneMatch(v->v==DimensionCompatibility.INCOMPATIBLE)&&dimensions.values().stream().anyMatch(v->v==DimensionCompatibility.COMPATIBLE);}
 public boolean crossProfile(String declaredProfession){return compatible()&&!profile.profession().equals(declaredProfession);}
}
