package operationalControl;

import banking.identity.BankAccountId;
import banking.identity.ConsumerId;
import banking.identity.InstitutionalAccountId;
import banking.identity.Profession;
import coinProperties.Currency;
import consumableRegistry.ConsumableType;
import consumerRegistry.Consumer;
import transaction.TransactionId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable operational-control decision with the institutional context frozen at decision time.
 * Mid relies on this record as historical evidence and must never reconstruct its actor from current account state.
 */
public record OperationalDecisionRecord(
        TransactionId transactionId,
        BankAccountId accountId,
        Optional<ConsumerId> consumerId,
        Optional<InstitutionalAccountId> institutionalAccountId,
        Optional<Profession> profession,
        Optional<BankAccountId> counterpartyAccountId,
        Optional<ConsumerId> counterpartyConsumerId,
        Optional<InstitutionalAccountId> counterpartyInstitutionalAccountId,
        MonetaryOperationType operationType,
        Currency currency,
        Optional<Currency> targetCurrency,
        Optional<ConsumableType> consumableType,
        int amount,
        OperationalControlSnapshot snapshot,
        Instant recordedAt
) {
    public OperationalDecisionRecord {
        Objects.requireNonNull(transactionId, "transaction id must not be null");
        Objects.requireNonNull(accountId, "account id must not be null");
        consumerId = requireOptional(consumerId, "consumer id");
        institutionalAccountId = requireOptional(institutionalAccountId, "institutional account id");
        profession = requireOptional(profession, "profession");
        counterpartyAccountId = requireOptional(counterpartyAccountId, "counterparty account id");
        counterpartyConsumerId = requireOptional(counterpartyConsumerId, "counterparty consumer id");
        counterpartyInstitutionalAccountId = requireOptional(counterpartyInstitutionalAccountId, "counterparty institutional account id");
        Objects.requireNonNull(operationType, "operation type must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        targetCurrency = requireOptional(targetCurrency, "target currency");
        consumableType = requireOptional(consumableType, "consumable type");
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        Objects.requireNonNull(recordedAt, "recorded at must not be null");
        if (amount <= 0) throw new IllegalArgumentException("operational decision amount must be positive");
        if (!snapshot.decidedAt().equals(recordedAt)) {
            throw new IllegalArgumentException("recordedAt must equal the snapshot decision instant");
        }
        if (operationType == MonetaryOperationType.EXCHANGE && targetCurrency.isEmpty()) {
            throw new IllegalArgumentException("exchange decisions must preserve the target currency");
        }
        if (operationType != MonetaryOperationType.EXCHANGE && targetCurrency.isPresent()) {
            throw new IllegalArgumentException("only exchange decisions may preserve a target currency");
        }
        boolean commercial = operationType == MonetaryOperationType.PURCHASE || operationType == MonetaryOperationType.SALE;
        if (commercial && consumableType.isEmpty()) {
            throw new IllegalArgumentException("commercial decisions must preserve the consumable type");
        }
        if (!commercial && consumableType.isPresent()) {
            throw new IllegalArgumentException("only commercial decisions may preserve a consumable type");
        }
        if (accountId.equals(counterpartyAccountId.orElse(null))) {
            throw new IllegalArgumentException("actor and counterparty accounts must be different");
        }
        boolean anyCounterparty = counterpartyAccountId.isPresent()
                || counterpartyConsumerId.isPresent()
                || counterpartyInstitutionalAccountId.isPresent();
        boolean completeCounterparty = counterpartyAccountId.isPresent()
                && counterpartyConsumerId.isPresent()
                && counterpartyInstitutionalAccountId.isPresent();
        if (anyCounterparty && !completeCounterparty) {
            throw new IllegalArgumentException("counterparty context must be complete when present");
        }
    }

    /** Compatibility constructor for pre-Mid callers. New writes must use capture(). */
    public OperationalDecisionRecord(TransactionId transactionId, BankAccountId accountId,
                                     MonetaryOperationType operationType, Currency currency, int amount,
                                     OperationalControlSnapshot snapshot, Instant recordedAt) {
        this(transactionId, accountId, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), operationType, currency,
                Optional.empty(), Optional.empty(), amount, snapshot, recordedAt);
    }

    public static OperationalDecisionRecord capture(TransactionId transactionId,
                                                    Consumer actor,
                                                    Optional<Consumer> counterparty,
                                                    MonetaryOperationType operationType,
                                                    Currency currency,
                                                    Optional<Currency> targetCurrency,
                                                    Optional<ConsumableType> consumableType,
                                                    int amount,
                                                    OperationalControlSnapshot snapshot,
                                                    Instant recordedAt) {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(counterparty, "counterparty optional must not be null");
        return new OperationalDecisionRecord(
                transactionId,
                actor.getBankAccount().getBankAccountId(),
                Optional.of(actor.getStableConsumerId()),
                Optional.of(actor.getBankAccount().getInstitutionalAccountId()),
                Optional.of(actor.getBankAccount().getProfession()),
                counterparty.map(value -> value.getBankAccount().getBankAccountId()),
                counterparty.map(Consumer::getStableConsumerId),
                counterparty.map(value -> value.getBankAccount().getInstitutionalAccountId()),
                operationType,
                currency,
                targetCurrency,
                consumableType,
                amount,
                snapshot,
                recordedAt);
    }

    public boolean enriched() {
        return consumerId.isPresent() && institutionalAccountId.isPresent() && profession.isPresent();
    }

    public String sourceId() {
        return transactionId + "|" + accountId + "|" + operationType.name();
    }

    private static <T> Optional<T> requireOptional(Optional<T> value, String label) {
        return Objects.requireNonNull(value, label + " optional must not be null");
    }
}
