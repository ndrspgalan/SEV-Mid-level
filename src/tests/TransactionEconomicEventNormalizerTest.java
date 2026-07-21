package tests;

import coinProperties.Currency;
import coinProperties.Material;
import coinProperties.SealType;
import coinProperties.Weight;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;
import economicEvent.EconomicEvent;
import economicEvent.EconomicEventStatus;
import economicEvent.EconomicEventType;
import economicEvent.normalization.*;
import transaction.*;
import transfer.TransferRequestId;

import java.time.Instant;
import java.util.Optional;

/** Executable contract test for Mid M1.2 transaction-ledger normalization. */
public final class TransactionEconomicEventNormalizerTest {
    private TransactionEconomicEventNormalizerTest() {}

    public static void main(String[] args) {
        ConsumerRegistry registry = new ConsumerRegistry();
        Consumer buyer = registry.register("buyer", "Kenan", "Mendigo");
        Consumer seller = registry.register("seller", "Jacob", "Mercader");
        TransactionEconomicEventNormalizer normalizer = new TransactionEconomicEventNormalizer(registry);

        normalizesMint(normalizer, buyer);
        normalizesExchange(normalizer, buyer);
        normalizesPurchase(normalizer, buyer, seller);
        normalizesTransfer(normalizer, buyer, seller);
        preservesRejectedOutcome(normalizer, buyer, seller);
        reportsUnresolvedActor(normalizer);
        System.out.println("TransactionEconomicEventNormalizerTest: PASSED");
    }

    private static void normalizesMint(TransactionEconomicEventNormalizer normalizer, Consumer consumer) {
        TransactionRecord record = new TransactionRecord(TransactionId.generate(), Instant.parse("2026-07-20T10:00:00Z"),
                TransactionType.MINT, TransactionStatus.COMPLETED,
                new MintTransactionDetails(Optional.of(consumer.getConsumerId()), Currency.VALERITA,
                        Material.COPPER, Weight.ONE, SealType.V, 10, 1.0, 0.0, 0.0,
                        Optional.of(10), Optional.of(0), Optional.empty()));
        EconomicEvent event = event(normalizer.normalize(record));
        check(event.type() == EconomicEventType.MONETARY_MINTED, "mint type");
        check(event.primaryAmount().orElseThrow().amount() == 10, "mint quantity");
        check(event.actor().consumerId().equals(consumer.getStableConsumerId()), "mint actor");
        check(event.source().sourceId().equals(record.id().toString()), "mint source");
    }

    private static void normalizesExchange(TransactionEconomicEventNormalizer normalizer, Consumer consumer) {
        TransactionRecord record = new TransactionRecord(TransactionId.generate(), Instant.parse("2026-07-20T10:01:00Z"),
                TransactionType.EXCHANGE, TransactionStatus.COMPLETED,
                new ExchangeTransactionDetails(consumer.getConsumerId(), Currency.VALERITA, Currency.SUELDO, 10,
                        Optional.of(2), Optional.of(20), Optional.of(10), Optional.of(0), Optional.of(2), Optional.empty()));
        EconomicEvent event = event(normalizer.normalize(record));
        check(event.type() == EconomicEventType.CURRENCY_EXCHANGED, "exchange type");
        check(event.primaryAmount().orElseThrow().currency() == Currency.VALERITA, "source currency");
        check(event.secondaryAmount().orElseThrow().currency() == Currency.SUELDO, "target currency");
        check(event.secondaryAmount().orElseThrow().amount() == 2, "target quantity");
    }

    private static void normalizesPurchase(TransactionEconomicEventNormalizer normalizer, Consumer buyer, Consumer seller) {
        TransactionRecord record = new TransactionRecord(TransactionId.generate(), Instant.parse("2026-07-20T10:02:00Z"),
                TransactionType.PURCHASE, TransactionStatus.COMPLETED,
                new PurchaseTransactionDetails(buyer.getConsumerId(), seller.getConsumerId(), "bread",
                        Optional.of(Currency.SUELDO), Optional.of(3), Optional.of(10), Optional.of(7),
                        Optional.of(4), Optional.of(7), Optional.empty()));
        EconomicEvent event = event(normalizer.normalize(record));
        check(event.type() == EconomicEventType.PURCHASE_EXECUTED, "purchase type");
        check(event.counterparty().orElseThrow().consumerId().orElseThrow().equals(seller.getStableConsumerId()), "seller counterparty");
        check(event.productCategory().orElseThrow().equals("bread"), "product category");
    }

    private static void normalizesTransfer(TransactionEconomicEventNormalizer normalizer, Consumer source, Consumer destination) {
        TransactionRecord record = new TransactionRecord(TransactionId.generate(), Instant.parse("2026-07-20T10:03:00Z"),
                TransactionType.TRANSFER, TransactionStatus.COMPLETED,
                new TransferTransactionDetails(TransferRequestId.generate(), source.getConsumerId(), destination.getConsumerId(),
                        Currency.BERYLARE, 5, "debt", Optional.of(9), Optional.of(4), Optional.of(1), Optional.of(6), Optional.empty()));
        EconomicEvent event = event(normalizer.normalize(record));
        check(event.type() == EconomicEventType.FUNDS_TRANSFERRED, "transfer type");
        check(event.primaryAmount().orElseThrow().amount() == 5, "transfer amount");
        check(event.attributes().get("reference").equals("debt"), "transfer reference");
    }

    private static void preservesRejectedOutcome(TransactionEconomicEventNormalizer normalizer, Consumer source, Consumer destination) {
        TransactionRecord record = new TransactionRecord(TransactionId.generate(), Instant.parse("2026-07-20T10:04:00Z"),
                TransactionType.TRANSFER, TransactionStatus.REJECTED,
                new TransferTransactionDetails(TransferRequestId.generate(), source.getConsumerId(), destination.getConsumerId(),
                        Currency.REAL_A5, 7, "blocked", Optional.of(9), Optional.empty(), Optional.of(1), Optional.empty(),
                        Optional.of("ACCOUNT_BLOCKED")));
        EconomicEvent event = event(normalizer.normalize(record));
        check(event.status() == EconomicEventStatus.REJECTED, "rejected status");
        check(event.rejectionReason().orElseThrow().equals("ACCOUNT_BLOCKED"), "rejection reason");
    }

    private static void reportsUnresolvedActor(TransactionEconomicEventNormalizer normalizer) {
        TransactionRecord record = new TransactionRecord(TransactionId.generate(), Instant.parse("2026-07-20T10:05:00Z"),
                TransactionType.EXCHANGE, TransactionStatus.REJECTED,
                new ExchangeTransactionDetails("unknown", Currency.VALERITA, Currency.SUELDO, 10,
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.of("CONSUMER_NOT_FOUND")));
        EconomicEventNormalizationResult result = normalizer.normalize(record);
        check(result instanceof EconomicEventNormalizationFailure, "unresolved actor must fail explicitly");
        EconomicEventNormalizationFailure failure = (EconomicEventNormalizationFailure) result;
        check(failure.reason() == EconomicEventNormalizationFailureReason.MISSING_ACTOR, "missing actor reason");
    }

    private static EconomicEvent event(EconomicEventNormalizationResult result) {
        if (!(result instanceof EconomicEventNormalizationSuccess success)) {
            throw new AssertionError("expected normalization success but got " + result);
        }
        return success.events().get(0);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
