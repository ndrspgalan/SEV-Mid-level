package economicEvent.normalization;

import banking.identity.InstitutionalAccountId;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;
import economicEvent.*;
import transaction.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Converts immutable transaction-ledger records into canonical Mid analytical events. */
public final class TransactionEconomicEventNormalizer implements EconomicEventNormalizer<TransactionRecord> {
    private final ConsumerRegistry consumerRegistry;

    public TransactionEconomicEventNormalizer(ConsumerRegistry consumerRegistry) {
        this.consumerRegistry = Objects.requireNonNull(consumerRegistry, "consumer registry must not be null");
    }

    @Override
    public EconomicEventNormalizationResult normalize(TransactionRecord record) {
        Objects.requireNonNull(record, "transaction record must not be null");
        try {
            return switch (record.type()) {
                case MINT -> normalizeMint(record, (MintTransactionDetails) record.details());
                case EXCHANGE -> normalizeExchange(record, (ExchangeTransactionDetails) record.details());
                case PURCHASE -> normalizePurchase(record, (PurchaseTransactionDetails) record.details());
                case TRANSFER -> normalizeTransfer(record, (TransferTransactionDetails) record.details());
            };
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return failure(record, EconomicEventNormalizationFailureReason.INCONSISTENT_SOURCE_DATA, exception.getMessage());
        }
    }

    private EconomicEventNormalizationResult normalizeMint(TransactionRecord record, MintTransactionDetails details) {
        if (details.consumerId().isEmpty()) {
            return failure(record, EconomicEventNormalizationFailureReason.MISSING_ACTOR,
                    "mint transaction does not preserve an account-holder identifier");
        }
        Optional<Consumer> actor = consumerRegistry.findById(details.consumerId().orElseThrow());
        if (actor.isEmpty()) return missingActor(record, details.consumerId().orElseThrow());
        if (details.coinQuantity().isEmpty()) {
            return failure(record, EconomicEventNormalizationFailureReason.MISSING_AMOUNT,
                    "mint transaction does not preserve the minted coin quantity");
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("material", details.material().name());
        attributes.put("coinWeight", details.coinWeight().name());
        attributes.put("sealType", details.sealType().name());
        attributes.put("totalWeightInGrams", Integer.toString(details.totalWeightInGrams()));
        details.remainingGrams().ifPresent(value -> attributes.put("remainingGrams", value.toString()));
        return success(record, EconomicEventType.MONETARY_MINTED, EconomicEventCategory.MONETARY,
                actor.get(), Optional.empty(),
                Optional.of(new EconomicAmount(details.currency(), details.coinQuantity().orElseThrow())), Optional.empty(),
                Optional.empty(), attributes);
    }

    private EconomicEventNormalizationResult normalizeExchange(TransactionRecord record, ExchangeTransactionDetails details) {
        Optional<Consumer> actor = consumerRegistry.findById(details.consumerId());
        if (actor.isEmpty()) return missingActor(record, details.consumerId());
        if (details.targetQuantity().isEmpty()) {
            return failure(record, EconomicEventNormalizationFailureReason.MISSING_AMOUNT,
                    "exchange transaction does not preserve the target quantity");
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        putBalances(attributes, details.sourceBalanceBefore(), details.sourceBalanceAfter(), "source");
        putBalances(attributes, details.targetBalanceBefore(), details.targetBalanceAfter(), "target");
        return success(record, EconomicEventType.CURRENCY_EXCHANGED, EconomicEventCategory.MONETARY,
                actor.get(), Optional.empty(),
                Optional.of(new EconomicAmount(details.sourceCurrency(), details.sourceQuantity())),
                Optional.of(new EconomicAmount(details.targetCurrency(), details.targetQuantity().orElseThrow())),
                Optional.empty(), attributes);
    }

    private EconomicEventNormalizationResult normalizePurchase(TransactionRecord record, PurchaseTransactionDetails details) {
        Optional<Consumer> buyer = consumerRegistry.findById(details.buyerId());
        if (buyer.isEmpty()) return missingActor(record, details.buyerId());
        Optional<Consumer> seller = consumerRegistry.findById(details.sellerId());
        if (seller.isEmpty()) {
            return failure(record, EconomicEventNormalizationFailureReason.MISSING_COUNTERPARTY,
                    "seller cannot be resolved: " + details.sellerId());
        }
        if (details.currency().isEmpty()) return failure(record, EconomicEventNormalizationFailureReason.MISSING_CURRENCY,
                "purchase transaction does not preserve its currency");
        if (details.price().isEmpty()) return failure(record, EconomicEventNormalizationFailureReason.MISSING_AMOUNT,
                "purchase transaction does not preserve its price");
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("consumableId", details.consumableId());
        details.consumableName().ifPresent(value -> attributes.put("consumableName", value));
        details.consumableCategory().ifPresent(value -> attributes.put("consumableCategory", value.name()));
        details.quantity().ifPresent(value -> attributes.put("quantity", value.toString()));
        details.unitPrice().ifPresent(value -> attributes.put("unitPrice", value.toString()));
        putBalances(attributes, details.buyerBalanceBefore(), details.buyerBalanceAfter(), "buyer");
        putBalances(attributes, details.sellerBalanceBefore(), details.sellerBalanceAfter(), "seller");
        return success(record, EconomicEventType.PURCHASE_EXECUTED, EconomicEventCategory.COMMERCIAL,
                buyer.get(), Optional.of(counterparty(seller.get())),
                Optional.of(new EconomicAmount(details.currency().orElseThrow(), details.price().orElseThrow())), Optional.empty(),
                details.consumableCategory().map(Enum::name).or(() -> Optional.of(details.consumableId())), attributes);
    }

    private EconomicEventNormalizationResult normalizeTransfer(TransactionRecord record, TransferTransactionDetails details) {
        Optional<Consumer> source = consumerRegistry.findById(details.sourceConsumerId());
        if (source.isEmpty()) return missingActor(record, details.sourceConsumerId());
        Optional<Consumer> destination = consumerRegistry.findById(details.destinationConsumerId());
        if (destination.isEmpty()) {
            return failure(record, EconomicEventNormalizationFailureReason.MISSING_COUNTERPARTY,
                    "destination consumer cannot be resolved: " + details.destinationConsumerId());
        }
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("requestId", details.requestId().toString());
        attributes.put("reference", details.reference());
        putBalances(attributes, details.sourceBalanceBefore(), details.sourceBalanceAfter(), "source");
        putBalances(attributes, details.destinationBalanceBefore(), details.destinationBalanceAfter(), "destination");
        return success(record, EconomicEventType.FUNDS_TRANSFERRED, EconomicEventCategory.TRANSFER,
                source.get(), Optional.of(counterparty(destination.get())),
                Optional.of(new EconomicAmount(details.currency(), details.quantity())), Optional.empty(),
                Optional.empty(), attributes);
    }

    private EconomicEventNormalizationResult success(TransactionRecord record,
                                                       EconomicEventType type,
                                                       EconomicEventCategory category,
                                                       Consumer actor,
                                                       Optional<EconomicCounterparty> counterparty,
                                                       Optional<EconomicAmount> primary,
                                                       Optional<EconomicAmount> secondary,
                                                       Optional<String> productCategory,
                                                       Map<String, String> attributes) {
        EconomicEventSource source = new EconomicEventSource(EconomicEventSourceType.TRANSACTION_LEDGER,
                record.id().toString(), record.details().getClass().getSimpleName());
        EconomicEventStatus status = record.status() == TransactionStatus.COMPLETED
                ? EconomicEventStatus.SUCCEEDED : EconomicEventStatus.REJECTED;
        Optional<String> rejection = rejectionCode(record.details());
        if (status == EconomicEventStatus.REJECTED && rejection.isEmpty()) {
            return failure(record, EconomicEventNormalizationFailureReason.INCONSISTENT_SOURCE_DATA,
                    "rejected transaction does not preserve a rejection code");
        }
        EconomicEvent event = new EconomicEvent(source.eventId(), record.occurredAt(), type, category, status,
                actor(actor), counterparty, primary, secondary,
                Optional.of(actor.getBankAccount().getProfession()), productCategory,
                status == EconomicEventStatus.REJECTED ? rejection : Optional.empty(), source, attributes);
        return new EconomicEventNormalizationSuccess(event);
    }

    private Optional<String> rejectionCode(TransactionDetails details) {
        if (details instanceof MintTransactionDetails value) return value.rejectionCode();
        if (details instanceof ExchangeTransactionDetails value) return value.rejectionCode();
        if (details instanceof PurchaseTransactionDetails value) return value.rejectionCode();
        if (details instanceof TransferTransactionDetails value) return value.rejectionCode();
        return Optional.empty();
    }

    private EconomicActor actor(Consumer consumer) {
        return new EconomicActor(consumer.getBankAccount().getBankAccountId(), consumer.getStableConsumerId(),
                Optional.of(consumer.getBankAccount().getInstitutionalAccountId()));
    }

    private EconomicCounterparty counterparty(Consumer consumer) {
        return new EconomicCounterparty(consumer.getBankAccount().getBankAccountId(),
                Optional.of(consumer.getStableConsumerId()),
                Optional.of(consumer.getBankAccount().getInstitutionalAccountId()));
    }

    private EconomicEventNormalizationFailure missingActor(TransactionRecord record, String id) {
        return failure(record, EconomicEventNormalizationFailureReason.MISSING_ACTOR,
                "transaction actor cannot be resolved: " + id);
    }

    private EconomicEventNormalizationFailure failure(TransactionRecord record,
                                                       EconomicEventNormalizationFailureReason reason,
                                                       String detail) {
        return new EconomicEventNormalizationFailure(EconomicEventSourceType.TRANSACTION_LEDGER,
                record.id().toString(), reason, detail == null ? reason.name() : detail);
    }

    private static void putBalances(Map<String, String> attributes, Optional<Integer> before,
                                    Optional<Integer> after, String prefix) {
        before.ifPresent(value -> attributes.put(prefix + "BalanceBefore", value.toString()));
        after.ifPresent(value -> attributes.put(prefix + "BalanceAfter", value.toString()));
    }
}
