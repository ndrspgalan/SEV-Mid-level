package operationalControl.profile;

import banking.identity.Profession;

/**
 * Firewall between banking professions and the canonical Junior credit matrix.
 *
 * <p>The String representation in Junior is an intentional domain mechanism:
 * professions evolve and their names participate in institutional account
 * identification. Mid therefore does not replace it with a second taxonomy; it
 * only centralizes access so analytics cannot duplicate credit thresholds.</p>
 */
public interface ProfessionCreditProfileResolver {
    ProfessionCreditProfile resolve(Profession profession);
}
