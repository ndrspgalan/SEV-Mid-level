package tests;

import application.behavior.BehaviorProfileService;
import banking.identity.BankAccountId;
import banking.identity.ConsumerId;
import behavior.aggregation.BehaviorAggregator;
import behavior.repository.InMemoryBehaviorProfileRepository;
import coinProperties.Currency;
import economicEvent.*;
import economicEvent.repository.InMemoryEconomicEventRepository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

public final class BehaviorProfileServiceTest {
    private BehaviorProfileServiceTest() {}

    public static void main(String[] args) {
        InMemoryEconomicEventRepository events = new InMemoryEconomicEventRepository();
        InMemoryBehaviorProfileRepository profiles = new InMemoryBehaviorProfileRepository();
        BehaviorProfileService service = new BehaviorProfileService(events, profiles, new BehaviorAggregator());
        ConsumerId consumer = ConsumerId.random();
        EconomicEventSource source = new EconomicEventSource(EconomicEventSourceType.TRANSACTION_LEDGER, "MINT-1", "MintTransactionDetails");
        events.save(new EconomicEvent(source.eventId(), Instant.parse("2026-02-01T00:00:00Z"), EconomicEventType.MONETARY_MINTED,
                EconomicEventCategory.MONETARY, EconomicEventStatus.SUCCEEDED,
                new EconomicActor(BankAccountId.random(), consumer), Optional.empty(),
                Optional.of(new EconomicAmount(Currency.VALERITA, 20)), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), source, Map.of("material", "COPPER")));

        check(service.rebuildProfiles().size() == 1, "one rebuilt profile");
        check(service.count() == 1, "repository count");
        check(service.findByConsumerId(consumer).orElseThrow().totalEvents() == 1, "query by consumer");
        service.rebuildProfiles();
        check(service.count() == 1, "idempotent replacement");
        System.out.println("BehaviorProfileServiceTest: PASSED");
    }

    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
