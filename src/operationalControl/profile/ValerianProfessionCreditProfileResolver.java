package operationalControl.profile;

import banking.identity.Profession;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Resolves the existing Junior profile without leaking textual joins into Mid analytics. */
public final class ValerianProfessionCreditProfileResolver implements ProfessionCreditProfileResolver {
    private final Map<String, ProfessionCreditProfile> byProfession;

    public ValerianProfessionCreditProfileResolver() {
        LinkedHashMap<String, ProfessionCreditProfile> profiles = new LinkedHashMap<>();
        for (ProfessionCreditProfile profile : ValerianProfessionCreditProfiles.all()) {
            profiles.put(profile.profession(), profile);
        }
        this.byProfession = Map.copyOf(profiles);
    }

    @Override
    public ProfessionCreditProfile resolve(Profession profession) {
        Objects.requireNonNull(profession, "profession must not be null");
        ProfessionCreditProfile profile = byProfession.get(profession.name());
        if (profile == null) throw new IllegalArgumentException("no credit profile for profession: " + profession.name());
        return profile;
    }
}
