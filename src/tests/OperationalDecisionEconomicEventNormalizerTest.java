package tests;

import coinProperties.Currency;
import consumableRegistry.ConsumableType;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;
import economicEvent.EconomicEvent;
import economicEvent.EconomicEventStatus;
import economicEvent.EconomicEventType;
import economicEvent.normalization.*;
import operationalControl.*;
import transaction.TransactionId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Executable contract test for Mid M1.4 enriched operational-decision normalization. */
public final class OperationalDecisionEconomicEventNormalizerTest {
    private OperationalDecisionEconomicEventNormalizerTest() {}

    public static void main(String[] args) {
        ConsumerRegistry registry = new ConsumerRegistry();
        Consumer buyer = registry.register("buyer", "Kenan", "Mendigo");
        Consumer seller = registry.register("seller", "Jacob", "Mercader");
        OperationalDecisionEconomicEventNormalizer normalizer = new OperationalDecisionEconomicEventNormalizer();

        normalizesAuthorizedDecision(normalizer, buyer, seller);
        normalizesRejectedDecision(normalizer, buyer);
        preservesExchangeContext(normalizer, buyer);
        reportsLegacyUnenrichedDecision(normalizer, buyer);
        rejectsInconsistentDecisionSnapshot(buyer);
        System.out.println("OperationalDecisionEconomicEventNormalizerTest: PASSED");
    }

    private static void normalizesAuthorizedDecision(OperationalDecisionEconomicEventNormalizer normalizer,
                                                      Consumer buyer, Consumer seller) {
        Instant at = Instant.parse("2026-07-20T11:00:00Z");
        OperationalControlSnapshot snapshot = new OperationalControlSnapshot(at, true,
                List.of(OperationalPolicyId.generate()), 7, 2, 12, 3, Optional.empty());
        OperationalDecisionRecord record = OperationalDecisionRecord.capture(
                TransactionId.generate(), buyer, Optional.of(seller), MonetaryOperationType.PURCHASE,
                Currency.SUELDO, Optional.empty(), Optional.of(ConsumableType.BASIC_NECESSITY),
                5, snapshot, at);

        check(record.enriched(), "captured record must be enriched");
        check(record.consumerId().orElseThrow().equals(buyer.getStableConsumerId()), "stable actor id");
        check(record.profession().orElseThrow().equals(buyer.getBankAccount().getProfession()), "frozen profession");
        check(record.counterpartyConsumerId().orElseThrow().equals(seller.getStableConsumerId()), "counterparty context");

        EconomicEvent event = event(normalizer.normalize(record));
        check(event.type() == EconomicEventType.OPERATION_AUTHORIZED, "authorized type");
        check(event.status() == EconomicEventStatus.SUCCEEDED, "authorized status");
        check(event.actor().consumerId().equals(buyer.getStableConsumerId()), "event actor");
        check(event.counterparty().orElseThrow().consumerId().orElseThrow().equals(seller.getStableConsumerId()), "event counterparty");
        check(event.primaryAmount().orElseThrow().amount() == 5, "decision amount");
        check(event.productCategory().orElseThrow().equals(ConsumableType.BASIC_NECESSITY.name()), "consumable context");
        check(event.attributes().get("usageAmountBefore").equals("7"), "usage before");
        check(event.source().sourceId().equals(record.sourceId()), "deterministic source id");
    }

    private static void normalizesRejectedDecision(OperationalDecisionEconomicEventNormalizer normalizer,
                                                    Consumer actor) {
        Instant at = Instant.parse("2026-07-20T11:01:00Z");
        OperationalControlSnapshot snapshot = new OperationalControlSnapshot(at, false,
                List.of(), 20, 4, 30, 5,
                Optional.of(OperationalControlRejectionReason.PERIOD_AMOUNT_LIMIT_EXCEEDED));
        OperationalDecisionRecord record = OperationalDecisionRecord.capture(
                TransactionId.generate(), actor, Optional.empty(), MonetaryOperationType.MINT,
                Currency.VALERITA, Optional.empty(), Optional.empty(), 10, snapshot, at);
        EconomicEvent event = event(normalizer.normalize(record));
        check(event.type() == EconomicEventType.OPERATION_REJECTED, "rejected type");
        check(event.status() == EconomicEventStatus.REJECTED, "rejected status");
        check(event.rejectionReason().orElseThrow().equals("PERIOD_AMOUNT_LIMIT_EXCEEDED"), "rejection reason");
        check(event.attributes().get("appliedPolicyIds").equals("NONE"), "empty policy set representation");
    }

    private static void preservesExchangeContext(OperationalDecisionEconomicEventNormalizer normalizer,
                                                 Consumer actor) {
        Instant at = Instant.parse("2026-07-20T11:02:00Z");
        OperationalControlSnapshot snapshot = new OperationalControlSnapshot(at, true,
                List.of(), 0, 0, 8, 1, Optional.empty());
        OperationalDecisionRecord record = OperationalDecisionRecord.capture(
                TransactionId.generate(), actor, Optional.empty(), MonetaryOperationType.EXCHANGE,
                Currency.VALERITA, Optional.of(Currency.SUELDO), Optional.empty(), 8, snapshot, at);
        EconomicEvent event = event(normalizer.normalize(record));
        check(event.attributes().get("targetCurrency").equals(Currency.SUELDO.name()), "target currency");
        check(event.actorProfession().orElseThrow().equals(actor.getBankAccount().getProfession()), "profession context");
    }

    private static void reportsLegacyUnenrichedDecision(OperationalDecisionEconomicEventNormalizer normalizer,
                                                        Consumer actor) {
        Instant at = Instant.parse("2026-07-20T11:03:00Z");
        OperationalControlSnapshot snapshot = new OperationalControlSnapshot(at, true,
                List.of(), 0, 0, 2, 1, Optional.empty());
        OperationalDecisionRecord legacy = new OperationalDecisionRecord(
                TransactionId.generate(), actor.getBankAccount().getBankAccountId(),
                MonetaryOperationType.MINT, Currency.VALERITA, 2, snapshot, at);
        EconomicEventNormalizationResult result = normalizer.normalize(legacy);
        check(result instanceof EconomicEventNormalizationFailure, "legacy record must fail explicitly");
        check(((EconomicEventNormalizationFailure) result).reason()
                == EconomicEventNormalizationFailureReason.MISSING_ACTOR, "missing actor reason");
    }

    private static void rejectsInconsistentDecisionSnapshot(Consumer actor) {
        Instant at = Instant.parse("2026-07-20T11:04:00Z");
        boolean failed = false;
        try {
            OperationalControlSnapshot snapshot = new OperationalControlSnapshot(at, true,
                    List.of(), 0, 0, 1, 1,
                    Optional.of(OperationalControlRejectionReason.PER_OPERATION_LIMIT_EXCEEDED));
            OperationalDecisionRecord record = OperationalDecisionRecord.capture(
                    TransactionId.generate(), actor, Optional.empty(), MonetaryOperationType.MINT,
                    Currency.VALERITA, Optional.empty(), Optional.empty(), 1, snapshot, at);
            EconomicEventNormalizationResult result = new OperationalDecisionEconomicEventNormalizer().normalize(record);
            failed = result instanceof EconomicEventNormalizationFailure;
        } catch (IllegalArgumentException expected) {
            failed = true;
        }
        check(failed, "inconsistent allowed snapshot must not normalize");
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
