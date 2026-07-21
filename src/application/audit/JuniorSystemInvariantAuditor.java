package application.audit;

import application.ValerianEconomicSystem;
import banking.identity.BankAccountId;
import banking.identity.ConsumerId;
import banking.identity.HolderStatus;
import banking.lifecycle.AccountOperationalStatus;
import coinProperties.Currency;
import consumerRegistry.Consumer;
import operationalControl.profile.ProfessionCreditProfile;
import operationalControl.profile.ValerianProfessionCreditProfiles;

import java.time.Clock;
import java.util.*;

/** Performs read-only, cross-module verification of the frozen Junior model. */
public final class JuniorSystemInvariantAuditor {
    private final Clock clock;

    public JuniorSystemInvariantAuditor(Clock clock) {
        this.clock = Objects.requireNonNull(clock);
    }

    public InvariantAuditReport audit(ValerianEconomicSystem system) {
        Objects.requireNonNull(system);
        List<InvariantViolation> violations = new ArrayList<>();
        auditAccounts(system, violations);
        auditCreditProfiles(violations);
        return new InvariantAuditReport(clock.instant(), violations);
    }

    private static void auditAccounts(ValerianEconomicSystem system, List<InvariantViolation> violations) {
        Set<ConsumerId> consumerIds = new HashSet<>();
        Set<BankAccountId> accountIds = new HashSet<>();
        Set<String> institutionalIds = new HashSet<>();

        for (Consumer consumer : system.getConsumerRegistry().all()) {
            var account = consumer.getBankAccount();
            String context = consumer.getName() + " / " + account.getInstitutionalAccountId();

            if (!consumerIds.add(consumer.getStableConsumerId())) {
                add(violations, "DUPLICATE_CONSUMER_ID", "El identificador estable de consumidor debe ser único.", context);
            }
            if (!accountIds.add(account.getBankAccountId())) {
                add(violations, "DUPLICATE_BANK_ACCOUNT_ID", "El identificador estable de cuenta debe ser único.", context);
            }
            if (!institutionalIds.add(account.getInstitutionalAccountId().toString())) {
                add(violations, "DUPLICATE_INSTITUTIONAL_ID", "El identificador institucional debe ser único.", context);
            }
            if (!system.getConsumerRegistry().getProfessionCatalog().all().contains(account.getProfession())) {
                add(violations, "UNKNOWN_ACCOUNT_PROFESSION", "La profesión de la cuenta debe pertenecer al catálogo canónico.", context);
            }
            for (Currency currency : Currency.values()) {
                if (account.getBalance(currency) < 0) {
                    add(violations, "NEGATIVE_BALANCE", "El saldo nunca puede ser negativo.", context + " / " + currency);
                }
            }
            if (account.getOperationalStatus() == AccountOperationalStatus.CLOSED && !account.hasZeroBalances()) {
                add(violations, "CLOSED_ACCOUNT_WITH_BALANCE", "Una cuenta cerrada debe mantener saldo cero.", context);
            }
            if (account.getOperationalStatus() == AccountOperationalStatus.CLOSED
                    && account.getHolderStatus() != HolderStatus.PENDING_NEW_HOLDER) {
                add(violations, "CLOSED_ACCOUNT_WITH_ASSIGNED_HOLDER", "Una cuenta cerrada no puede conservar titular asignado.", context);
            }
            if (account.isOperational() && (account.getOperationalStatus() != AccountOperationalStatus.ACTIVE
                    || account.getHolderStatus() != HolderStatus.ASSIGNED)) {
                add(violations, "INVALID_OPERATIONAL_PREDICATE", "La operatividad exige cuenta activa y titular asignado.", context);
            }
            if (system.getConsumerRegistry().findByAccountId(account.getBankAccountId()).orElse(null) != consumer) {
                add(violations, "ACCOUNT_INDEX_MISMATCH", "El índice estable de cuentas debe resolver al titular correcto.", context);
            }
            if (system.getConsumerRegistry().findById(account.getInstitutionalAccountId().toString()).orElse(null) != consumer) {
                add(violations, "INSTITUTIONAL_INDEX_MISMATCH", "El índice institucional debe resolver al titular correcto.", context);
            }
        }
    }

    private static void auditCreditProfiles(List<InvariantViolation> violations) {
        List<ProfessionCreditProfile> profiles = ValerianProfessionCreditProfiles.all();
        if (profiles.size() != 19) {
            add(violations, "INVALID_CANONICAL_PROFILE_COUNT", "La matriz canónica Junior debe contener diecinueve profesiones.", String.valueOf(profiles.size()));
        }
        Set<String> professions = new HashSet<>();
        Set<String> signatures = new HashSet<>();
        for (ProfessionCreditProfile profile : profiles) {
            if (!professions.add(profile.profession())) {
                add(violations, "DUPLICATE_PROFESSION_PROFILE", "Cada profesión debe tener un único perfil crediticio.", profile.profession());
            }
            String signature = profile.monthlyEconomicCapacity() + ":" + profile.dailyInteractionCount()
                    + ":" + profile.maximumMintCurrency() + ":" + profile.maximumExchangeCurrency()
                    + ":" + profile.maximumPurchaseCurrency() + ":" + profile.maximumTransferCurrency()
                    + ":" + profile.maximumConsumableType() + ":" + profile.unlimited();
            if (!signatures.add(signature)) {
                add(violations, "IDENTICAL_CREDIT_PROFILE", "Dos profesiones no deben resultar financieramente idénticas.", profile.profession());
            }
        }
    }

    private static void add(List<InvariantViolation> target, String code, String label, String context) {
        target.add(new InvariantViolation(code, label, context));
    }
}
