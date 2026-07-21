package application.account;

import banking.identity.InstitutionalAccountId;
import banking.identity.Profession;

import java.util.Optional;

public record ProfessionChangeResult(
        boolean completed,
        InstitutionalAccountId previousInstitutionalId,
        InstitutionalAccountId currentInstitutionalId,
        Profession previousProfession,
        Profession currentProfession,
        ProfessionChangeRejectionReason rejectionReason,
        String message
) {
    public Optional<ProfessionChangeRejectionReason> rejection() { return Optional.ofNullable(rejectionReason); }
    public static ProfessionChangeResult completed(InstitutionalAccountId previousId, InstitutionalAccountId newId,
                                                    Profession previousProfession, Profession newProfession) {
        return new ProfessionChangeResult(true, previousId, newId, previousProfession, newProfession, null, "Profession changed");
    }
    public static ProfessionChangeResult rejected(ProfessionChangeRejectionReason reason, String message) {
        return new ProfessionChangeResult(false, null, null, null, null, reason, message);
    }
}
