package consumerRegistry;

import banking.identity.*;
import banking.lifecycle.AccountOperationalStatus;
import coinProperties.Currency;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class BankAccount {
    private final BankAccountId bankAccountId;
    private InstitutionalAccountId institutionalAccountId;
    private Profession profession;
    private CensusPosition censusPosition;
    private ReuseSequence reuseSequence;
    private HolderStatus holderStatus;
    private AccountOperationalStatus operationalStatus;
    private final Map<Currency, Integer> balances = new EnumMap<>(Currency.class);

    public BankAccount(BankAccountId bankAccountId, InstitutionalAccountId institutionalAccountId,
                       Profession profession, CensusPosition censusPosition, ReuseSequence reuseSequence) {
        this.bankAccountId = Objects.requireNonNull(bankAccountId);
        this.institutionalAccountId = Objects.requireNonNull(institutionalAccountId);
        this.profession = Objects.requireNonNull(profession);
        this.censusPosition = Objects.requireNonNull(censusPosition);
        this.reuseSequence = Objects.requireNonNull(reuseSequence);
        this.holderStatus = HolderStatus.ASSIGNED;
        this.operationalStatus = AccountOperationalStatus.ACTIVE;
        for (Currency currency : Currency.values()) balances.put(currency, 0);
    }

    public BankAccountId getBankAccountId() { return bankAccountId; }
    public InstitutionalAccountId getInstitutionalAccountId() { return institutionalAccountId; }
    public Profession getProfession() { return profession; }
    public CensusPosition getCensusPosition() { return censusPosition; }
    public ReuseSequence getReuseSequence() { return reuseSequence; }
    public HolderStatus getHolderStatus() { return holderStatus; }
    public AccountOperationalStatus getOperationalStatus() { return operationalStatus; }
    public boolean isOperational() { return operationalStatus == AccountOperationalStatus.ACTIVE && holderStatus == HolderStatus.ASSIGNED; }

    public void changeInstitutionalIdentity(InstitutionalAccountId newId, Profession newProfession,
                                            CensusPosition newPosition, ReuseSequence newReuseSequence) {
        requireAssigned();
        this.institutionalAccountId = Objects.requireNonNull(newId);
        this.profession = Objects.requireNonNull(newProfession);
        this.censusPosition = Objects.requireNonNull(newPosition);
        this.reuseSequence = Objects.requireNonNull(newReuseSequence);
    }

    public void releaseHolder() {
        requireAssigned();
        if (operationalStatus == AccountOperationalStatus.CLOSED) throw new IllegalStateException("closed account cannot release holder");
        holderStatus = HolderStatus.PENDING_NEW_HOLDER;
    }

    public void assignHolder(InstitutionalAccountId newId, Profession newProfession,
                             CensusPosition newPosition, ReuseSequence newReuseSequence) {
        if (holderStatus != HolderStatus.PENDING_NEW_HOLDER) throw new IllegalStateException("account already has a holder");
        institutionalAccountId = Objects.requireNonNull(newId);
        profession = Objects.requireNonNull(newProfession);
        censusPosition = Objects.requireNonNull(newPosition);
        reuseSequence = Objects.requireNonNull(newReuseSequence);
        holderStatus = HolderStatus.ASSIGNED;
    }

    public void block() {
        requireAssigned();
        if (operationalStatus == AccountOperationalStatus.CLOSED) throw new IllegalStateException("closed account cannot be blocked");
        if (operationalStatus == AccountOperationalStatus.BLOCKED) throw new IllegalStateException("account already blocked");
        operationalStatus = AccountOperationalStatus.BLOCKED;
    }

    public void unblock() {
        requireAssigned();
        if (operationalStatus != AccountOperationalStatus.BLOCKED) throw new IllegalStateException("account is not blocked");
        operationalStatus = AccountOperationalStatus.ACTIVE;
    }

    public void close() {
        if (operationalStatus == AccountOperationalStatus.CLOSED) throw new IllegalStateException("account already closed");
        if (!hasZeroBalances()) throw new IllegalStateException("account balances must be zero before closing");
        operationalStatus = AccountOperationalStatus.CLOSED;
        holderStatus = HolderStatus.PENDING_NEW_HOLDER;
    }

    public int getBalance(Currency currency) { return balances.get(Objects.requireNonNull(currency)); }
    public void deposit(Currency currency, int quantity) {
        requireTransactionalAccess(); Objects.requireNonNull(currency);
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be greater than zero");
        balances.put(currency, Math.addExact(getBalance(currency), quantity));
    }
    public boolean withdraw(Currency currency, int quantity) {
        requireTransactionalAccess(); Objects.requireNonNull(currency);
        if (quantity <= 0 || getBalance(currency) < quantity) return false;
        balances.put(currency, getBalance(currency) - quantity); return true;
    }
    public boolean hasZeroBalances() { return balances.values().stream().allMatch(value -> value == 0); }
    private void requireTransactionalAccess() {
        requireAssigned();
        if (operationalStatus != AccountOperationalStatus.ACTIVE) throw new IllegalStateException("account is not active");
    }
    private void requireAssigned() {
        if (holderStatus != HolderStatus.ASSIGNED) throw new IllegalStateException("account is pending a new holder");
    }
}
