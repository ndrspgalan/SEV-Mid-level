package banking.census;

import banking.identity.BankAccountId;
import banking.identity.CensusPosition;
import banking.identity.Profession;
import banking.identity.ReuseSequence;

import java.util.Objects;
import java.util.Optional;

public final class ProfessionCensusSlot {
    private final Profession profession;
    private final CensusPosition position;
    private ReuseSequence reuseSequence;
    private CensusSlotStatus status;
    private BankAccountId accountId;

    public ProfessionCensusSlot(Profession profession, CensusPosition position, ReuseSequence reuseSequence, BankAccountId accountId) {
        this.profession = Objects.requireNonNull(profession);
        this.position = Objects.requireNonNull(position);
        this.reuseSequence = Objects.requireNonNull(reuseSequence);
        this.accountId = Objects.requireNonNull(accountId);
        this.status = CensusSlotStatus.OCCUPIED;
    }
    public Profession profession() { return profession; }
    public CensusPosition position() { return position; }
    public ReuseSequence reuseSequence() { return reuseSequence; }
    public CensusSlotStatus status() { return status; }
    public Optional<BankAccountId> accountId() { return Optional.ofNullable(accountId); }
    public void release() {
        if (status != CensusSlotStatus.OCCUPIED) throw new IllegalStateException("slot is not occupied");
        accountId = null;
        status = reuseSequence.value() == ReuseSequence.MAX ? CensusSlotStatus.EXHAUSTED : CensusSlotStatus.AVAILABLE;
    }
    public void occupy(BankAccountId newAccountId) {
        if (status != CensusSlotStatus.AVAILABLE) throw new IllegalStateException("slot is not available");
        reuseSequence = reuseSequence.next();
        accountId = Objects.requireNonNull(newAccountId);
        status = CensusSlotStatus.OCCUPIED;
    }
}
