package accountHistory;

import banking.identity.*;
import banking.lifecycle.AccountClosureReason;
import banking.lifecycle.AccountOperationalStatus;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class AccountHistoryEvent {
    private final AccountHistoryEventId eventId;
    private final BankAccountId bankAccountId;
    private final ConsumerId consumerId;
    private final AccountHistoryEventType type;
    private final AccountHistoryEventStatus status;
    private final Instant occurredAt;
    private final Profession previousProfession;
    private final Profession currentProfession;
    private final InstitutionalAccountId previousInstitutionalId;
    private final InstitutionalAccountId currentInstitutionalId;
    private final HolderStatus previousHolderStatus;
    private final HolderStatus currentHolderStatus;
    private final AccountOperationalStatus previousOperationalStatus;
    private final AccountOperationalStatus currentOperationalStatus;
    private final AccountClosureReason closureReason;
    private final String rejectionReason;
    private final String reference;

    public AccountHistoryEvent(AccountHistoryEventId eventId, BankAccountId bankAccountId, ConsumerId consumerId,
                               AccountHistoryEventType type, AccountHistoryEventStatus status, Instant occurredAt,
                               Profession previousProfession, Profession currentProfession,
                               InstitutionalAccountId previousInstitutionalId, InstitutionalAccountId currentInstitutionalId,
                               HolderStatus previousHolderStatus, HolderStatus currentHolderStatus,
                               String rejectionReason, String reference) {
        this(eventId, bankAccountId, consumerId, type, status, occurredAt, previousProfession, currentProfession,
                previousInstitutionalId, currentInstitutionalId, previousHolderStatus, currentHolderStatus,
                null, null, null, rejectionReason, reference);
    }

    public AccountHistoryEvent(AccountHistoryEventId eventId, BankAccountId bankAccountId, ConsumerId consumerId,
                               AccountHistoryEventType type, AccountHistoryEventStatus status, Instant occurredAt,
                               Profession previousProfession, Profession currentProfession,
                               InstitutionalAccountId previousInstitutionalId, InstitutionalAccountId currentInstitutionalId,
                               HolderStatus previousHolderStatus, HolderStatus currentHolderStatus,
                               AccountOperationalStatus previousOperationalStatus,
                               AccountOperationalStatus currentOperationalStatus,
                               AccountClosureReason closureReason,
                               String rejectionReason, String reference) {
        this.eventId = Objects.requireNonNull(eventId);
        this.bankAccountId = Objects.requireNonNull(bankAccountId);
        this.consumerId = Objects.requireNonNull(consumerId);
        this.type = Objects.requireNonNull(type);
        this.status = Objects.requireNonNull(status);
        this.occurredAt = Objects.requireNonNull(occurredAt);
        this.previousProfession = previousProfession;
        this.currentProfession = currentProfession;
        this.previousInstitutionalId = previousInstitutionalId;
        this.currentInstitutionalId = currentInstitutionalId;
        this.previousHolderStatus = previousHolderStatus;
        this.currentHolderStatus = currentHolderStatus;
        this.previousOperationalStatus = previousOperationalStatus;
        this.currentOperationalStatus = currentOperationalStatus;
        this.closureReason = closureReason;
        this.rejectionReason = normalizeOptional(rejectionReason);
        this.reference = normalizeRequired(reference, "reference");
        if (status == AccountHistoryEventStatus.REJECTED && this.rejectionReason == null) {
            throw new IllegalArgumentException("rejected event requires rejection reason");
        }
    }

    private static String normalizeOptional(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
    private static String normalizeRequired(String value, String label) {
        String normalized = Objects.requireNonNull(value, label + " must not be null").trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + " must not be blank");
        return normalized;
    }

    public AccountHistoryEventId eventId() { return eventId; }
    public BankAccountId bankAccountId() { return bankAccountId; }
    public ConsumerId consumerId() { return consumerId; }
    public AccountHistoryEventType type() { return type; }
    public AccountHistoryEventStatus status() { return status; }
    public Instant occurredAt() { return occurredAt; }
    public Optional<Profession> previousProfession() { return Optional.ofNullable(previousProfession); }
    public Optional<Profession> currentProfession() { return Optional.ofNullable(currentProfession); }
    public Optional<InstitutionalAccountId> previousInstitutionalId() { return Optional.ofNullable(previousInstitutionalId); }
    public Optional<InstitutionalAccountId> currentInstitutionalId() { return Optional.ofNullable(currentInstitutionalId); }
    public Optional<HolderStatus> previousHolderStatus() { return Optional.ofNullable(previousHolderStatus); }
    public Optional<HolderStatus> currentHolderStatus() { return Optional.ofNullable(currentHolderStatus); }
    public Optional<AccountOperationalStatus> previousOperationalStatus() { return Optional.ofNullable(previousOperationalStatus); }
    public Optional<AccountOperationalStatus> currentOperationalStatus() { return Optional.ofNullable(currentOperationalStatus); }
    public Optional<AccountClosureReason> closureReason() { return Optional.ofNullable(closureReason); }
    public Optional<String> rejectionReason() { return Optional.ofNullable(rejectionReason); }
    public String reference() { return reference; }
}
