package tests;

import banking.identity.BankAccountId;
import banking.identity.ConsumerId;
import behavior.aggregation.BehaviorAggregationException;
import behavior.aggregation.BehaviorAggregator;
import behavior.profile.BehaviorProfile;
import behavior.profile.ConsumableBehaviorProfile;
import coinProperties.Currency;
import consumableRegistry.ConsumableCategory;
import economicEvent.*;

import java.time.Instant;
import java.util.*;

public final class BehaviorAggregatorTest {
    private BehaviorAggregatorTest() {}

    public static void main(String[] args) {
        ConsumerId actorId = ConsumerId.random();
        BankAccountId actorAccount = BankAccountId.random();
        BankAccountId sellerAccount = BankAccountId.random();
        List<EconomicEvent> events = List.of(
                purchase("P1", "2026-01-01T10:00:00Z", actorId, actorAccount, sellerAccount, 2, 3, false),
                purchase("P2", "2026-01-05T10:00:00Z", actorId, actorAccount, sellerAccount, 1, 4, false),
                purchase("P3", "2026-01-06T10:00:00Z", actorId, actorAccount, sellerAccount, 1, 3, true),
                transfer("T1", actorId, actorAccount, sellerAccount)
        );

        BehaviorProfile profile = new BehaviorAggregator().aggregate(events).get(0);
        check(profile.totalEvents() == 4, "total events");
        check(profile.succeededEvents() == 3, "succeeded events");
        check(profile.rejectedEvents() == 1, "rejected events");
        check(profile.counterparties().contains(sellerAccount), "counterparty");
        check(profile.succeededVolumeByCurrency().get(Currency.VALERITA) == 15, "succeeded volume");
        ConsumableBehaviorProfile bread = profile.consumables().get("FOOD-001");
        check(bread.purchaseCount() == 2, "purchase count excludes rejected");
        check(bread.unitsPurchased() == 3, "units purchased");
        check(bread.totalSpentByCurrency().get(Currency.VALERITA) == 10, "spent");
        check(profile.consumableCategories().get(ConsumableCategory.FOOD).distinctConsumables() == 1, "category aggregate");

        try {
            new BehaviorAggregator().aggregate(List.of(malformedPurchase(actorId, actorAccount, sellerAccount)));
            throw new AssertionError("malformed purchase should fail");
        } catch (BehaviorAggregationException expected) {
            check(expected.getMessage().contains("quantity"), "explicit malformed-data failure");
        }
        System.out.println("BehaviorAggregatorTest: PASSED");
    }

    private static EconomicEvent purchase(String sourceId, String at, ConsumerId actorId, BankAccountId actorAccount,
                                          BankAccountId seller, int quantity, int unitPrice, boolean rejected) {
        Map<String,String> attrs = new LinkedHashMap<>();
        attrs.put("consumableId", "FOOD-001"); attrs.put("consumableName", "Pan");
        attrs.put("consumableCategory", "FOOD"); attrs.put("quantity", Integer.toString(quantity));
        attrs.put("unitPrice", Integer.toString(unitPrice));
        EconomicEventSource source = new EconomicEventSource(EconomicEventSourceType.TRANSACTION_LEDGER, sourceId, "PurchaseTransactionDetails");
        return new EconomicEvent(source.eventId(), Instant.parse(at), EconomicEventType.PURCHASE_EXECUTED,
                EconomicEventCategory.COMMERCIAL, rejected ? EconomicEventStatus.REJECTED : EconomicEventStatus.SUCCEEDED,
                new EconomicActor(actorAccount, actorId), Optional.of(new EconomicCounterparty(seller)),
                Optional.of(new EconomicAmount(Currency.VALERITA, quantity * unitPrice)), Optional.empty(), Optional.empty(),
                Optional.of("FOOD"), rejected ? Optional.of("TEST_REJECTION") : Optional.empty(), source, attrs);
    }

    private static EconomicEvent malformedPurchase(ConsumerId actorId, BankAccountId actorAccount, BankAccountId seller) {
        EconomicEvent event = purchase("BAD", "2026-01-01T00:00:00Z", actorId, actorAccount, seller, 1, 3, false);
        Map<String,String> attrs = new LinkedHashMap<>(event.attributes()); attrs.remove("quantity");
        return new EconomicEvent(event.id(), event.occurredAt(), event.type(), event.category(), event.status(), event.actor(),
                event.counterparty(), event.primaryAmount(), event.secondaryAmount(), event.actorProfession(), event.productCategory(),
                event.rejectionReason(), event.source(), attrs);
    }

    private static EconomicEvent transfer(String sourceId, ConsumerId actorId, BankAccountId actorAccount, BankAccountId destination) {
        EconomicEventSource source = new EconomicEventSource(EconomicEventSourceType.TRANSACTION_LEDGER, sourceId, "TransferTransactionDetails");
        return new EconomicEvent(source.eventId(), Instant.parse("2026-01-10T00:00:00Z"), EconomicEventType.FUNDS_TRANSFERRED,
                EconomicEventCategory.TRANSFER, EconomicEventStatus.SUCCEEDED, new EconomicActor(actorAccount, actorId),
                Optional.of(new EconomicCounterparty(destination)), Optional.of(new EconomicAmount(Currency.VALERITA, 5)),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), source, Map.of("reference", "test"));
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
