package tests;

import application.analytics.EconomicEventQueryService;
import application.analytics.EconomicEventStatisticsService;
import banking.identity.*;
import coinProperties.Currency;
import economicEvent.*;
import economicEvent.query.*;
import economicEvent.repository.*;

import java.time.Instant;
import java.util.*;

public final class EconomicEventRepositoryAndQueryTest {
    private static final BankAccountId ACCOUNT_A = BankAccountId.parse("00000000-0000-0000-0000-0000000000a1");
    private static final BankAccountId ACCOUNT_B = BankAccountId.parse("00000000-0000-0000-0000-0000000000b2");
    private static final BankAccountId ACCOUNT_C = BankAccountId.parse("00000000-0000-0000-0000-0000000000c3");
    private static final ConsumerId CONSUMER_A = ConsumerId.parse("10000000-0000-0000-0000-0000000000a1");
    private static final ConsumerId CONSUMER_B = ConsumerId.parse("10000000-0000-0000-0000-0000000000b2");
    private static final ConsumerId CONSUMER_C = ConsumerId.parse("10000000-0000-0000-0000-0000000000c3");
    private static final Profession FARMER = new Profession("Farmer", new ProfessionCode("Farmer"));
    private static final Profession SMITH = new Profession("Smith", new ProfessionCode("Smith"));

    private EconomicEventRepositoryAndQueryTest() {}

    public static void main(String[] args) {
        InMemoryEconomicEventRepository repository = new InMemoryEconomicEventRepository();
        EconomicEvent mint = mintEvent("tx-1", "2026-07-19T10:00:00Z", ACCOUNT_A, CONSUMER_A, 100);
        EconomicEvent transfer = transferEvent("tx-2", "2026-07-19T11:00:00Z", ACCOUNT_A, CONSUMER_A, ACCOUNT_B, CONSUMER_B, 30);
        EconomicEvent rejected = rejectedDecision("tx-3", "2026-07-19T12:00:00Z", ACCOUNT_C, CONSUMER_C, 70);
        EconomicEvent exchange = exchangeEvent("tx-4", "2026-07-19T13:00:00Z", ACCOUNT_B, CONSUMER_B, 5, 50);

        testRepositorySemantics(repository, mint, transfer, rejected, exchange);
        EconomicEventQueryService queryService = new EconomicEventQueryService(repository);
        testPaginationAndOrdering(queryService);
        testCombinedFilters(queryService);
        testTimeSourceAndRejectedFilters(queryService);
        testStatistics(queryService);
        testValidation();

        System.out.println("EconomicEventRepositoryAndQueryTest: PASSED");
    }

    private static void testRepositorySemantics(InMemoryEconomicEventRepository repository,
                                                EconomicEvent mint, EconomicEvent transfer,
                                                EconomicEvent rejected, EconomicEvent exchange) {
        assertEquals(EconomicEventSaveResult.CREATED, repository.save(mint), "first save");
        assertEquals(EconomicEventSaveResult.ALREADY_PRESENT, repository.save(mint), "idempotent save");
        EconomicEventBatchSaveResult batch = repository.saveAll(List.of(transfer, rejected, exchange, transfer));
        assertEquals(4, batch.inspected(), "batch inspected");
        assertEquals(3, batch.created(), "batch created");
        assertEquals(1, batch.alreadyPresent(), "batch duplicate");
        assertEquals(4L, repository.count(), "repository count");
        assertTrue(repository.exists(mint.id()), "exists");
        assertEquals(mint, repository.findById(mint.id()).orElseThrow(), "find by id");
        assertThrowsState(() -> repository.save(copyAt(mint, "2026-07-19T10:01:00Z")), "conflicting content");
        assertEquals(4L, repository.count(), "collision must not mutate repository");
        assertImmutable(repository.findAll(), "findAll immutable");
    }

    private static void testPaginationAndOrdering(EconomicEventQueryService service) {
        EconomicEventPage<EconomicEvent> page = service.search(EconomicEventQuery.all(new EconomicEventPageRequest(0, 2)));
        assertEquals(4L, page.totalElements(), "total elements");
        assertEquals(2, page.totalPages(), "total pages");
        assertEquals(EconomicEventType.CURRENCY_EXCHANGED, page.content().get(0).type(), "newest first");
        assertTrue(page.hasNext(), "has next");
        assertFalse(page.hasPrevious(), "no previous");
        assertImmutable(page.content(), "page immutable");

        EconomicEventPage<EconomicEvent> outOfRange = service.search(withPage(EconomicEventQuery.all(
                EconomicEventPageRequest.firstPage(10)), new EconomicEventPageRequest(9, 10)));
        assertTrue(outOfRange.isEmpty(), "out-of-range empty");
        assertEquals(4L, outOfRange.totalElements(), "out-of-range total preserved");
    }

    private static void testCombinedFilters(EconomicEventQueryService service) {
        EconomicEventQuery query = query(Optional.of(ACCOUNT_A), Optional.of(ACCOUNT_B), Optional.of(CONSUMER_B),
                Optional.of(FARMER), Optional.of(EconomicEventType.FUNDS_TRANSFERRED),
                Optional.of(EconomicEventCategory.TRANSFER), Optional.of(EconomicEventStatus.SUCCEEDED),
                Optional.of(Currency.VALERITA), Optional.of(25), Optional.of(35), Optional.empty(), Optional.empty(),
                Optional.of(EconomicEventSourceType.TRANSACTION_LEDGER), Optional.of("tx-2"), Optional.of(false));
        List<EconomicEvent> events = service.search(query).content();
        assertEquals(1, events.size(), "combined filters");
        assertEquals("tx-2", events.get(0).source().sourceId(), "filtered source");

        EconomicEventQuery exchangeBySecondaryCurrency = query(Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(Currency.VALERITA),
                Optional.of(40), Optional.of(60), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(1L, service.search(exchangeBySecondaryCurrency).totalElements(), "secondary amount and currency searchable");
    }

    private static void testTimeSourceAndRejectedFilters(EconomicEventQueryService service) {
        EconomicEventQuery query = query(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.of(Instant.parse("2026-07-19T11:00:00Z")), Optional.of(Instant.parse("2026-07-19T13:00:00Z")),
                Optional.empty(), Optional.empty(), Optional.empty());
        assertEquals(2L, service.search(query).totalElements(), "inclusive/exclusive time range");

        EconomicEventQuery rejected = query(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of(EconomicEventSourceType.OPERATIONAL_DECISION_JOURNAL),
                Optional.empty(), Optional.of(true));
        assertEquals(1L, service.search(rejected).totalElements(), "rejected operational event");
    }

    private static void testStatistics(EconomicEventQueryService queryService) {
        EconomicEventStatistics stats = new EconomicEventStatisticsService(queryService)
                .calculate(EconomicEventQuery.all(EconomicEventPageRequest.firstPage(1)));
        assertEquals(4L, stats.totalEvents(), "statistics ignore pagination");
        assertEquals(1L, stats.count(EconomicEventType.FUNDS_TRANSFERRED), "type count");
        assertEquals(1L, stats.count(EconomicEventStatus.REJECTED), "status count");
        assertEquals(250L, stats.volume(Currency.VALERITA), "Valerita volume includes primary and secondary");
        assertEquals(5L, stats.volume(Currency.SUELDO), "Sueldo volume");
        assertEquals(4L, stats.monetaryEvents(), "monetary events");
        assertEquals(1L, stats.rejectedEvents(), "rejected events");
        assertEquals(3L, stats.uniqueActorAccounts(), "unique actors");
        assertEquals(3L, stats.uniqueConsumers(), "unique consumers");
        assertImmutableMap(stats.byType(), "statistics map immutable");
    }

    private static void testValidation() {
        assertThrowsArgument(() -> new EconomicEventPageRequest(-1, 10), "negative page");
        assertThrowsArgument(() -> new EconomicEventPageRequest(0, 201), "oversized page");
        assertThrowsArgument(() -> query(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(50), Optional.of(10),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()), "invalid amount range");
        assertThrowsArgument(() -> new EconomicEventQuery(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.of(EconomicEventStatus.SUCCEEDED), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(true),
                EconomicEventSortDirection.NEWEST_FIRST, EconomicEventPageRequest.firstPage(10)), "contradictory rejected filter");
    }

    private static EconomicEventQuery query(Optional<BankAccountId> actor, Optional<BankAccountId> counterparty,
                                            Optional<ConsumerId> consumer, Optional<Profession> profession,
                                            Optional<EconomicEventType> type, Optional<EconomicEventCategory> category,
                                            Optional<EconomicEventStatus> status, Optional<Currency> currency,
                                            Optional<Integer> min, Optional<Integer> max, Optional<Instant> from,
                                            Optional<Instant> to, Optional<EconomicEventSourceType> sourceType,
                                            Optional<String> sourceId, Optional<Boolean> rejected) {
        return new EconomicEventQuery(actor, counterparty, consumer, profession, type, category, status, currency,
                min, max, from, to, sourceType, sourceId, rejected, EconomicEventSortDirection.NEWEST_FIRST,
                EconomicEventPageRequest.firstPage(100));
    }

    private static EconomicEventQuery withPage(EconomicEventQuery q, EconomicEventPageRequest page) {
        return new EconomicEventQuery(q.actorAccountId(), q.counterpartyAccountId(), q.consumerId(), q.actorProfession(),
                q.type(), q.category(), q.status(), q.currency(), q.minimumAmountInclusive(), q.maximumAmountInclusive(),
                q.occurredFromInclusive(), q.occurredToExclusive(), q.sourceType(), q.sourceId(), q.rejected(),
                q.sortDirection(), page);
    }

    private static EconomicEvent mintEvent(String sourceId, String instant, BankAccountId account, ConsumerId consumer, int amount) {
        return event(sourceId, instant, EconomicEventType.MONETARY_MINTED, EconomicEventCategory.MONETARY,
                EconomicEventStatus.SUCCEEDED, account, consumer, Optional.empty(), new EconomicAmount(Currency.VALERITA, amount),
                Optional.empty(), FARMER, EconomicEventSourceType.TRANSACTION_LEDGER, Optional.empty());
    }

    private static EconomicEvent transferEvent(String sourceId, String instant, BankAccountId actor, ConsumerId consumer,
                                                BankAccountId counterparty, ConsumerId counterpartyConsumer, int amount) {
        return event(sourceId, instant, EconomicEventType.FUNDS_TRANSFERRED, EconomicEventCategory.TRANSFER,
                EconomicEventStatus.SUCCEEDED, actor, consumer,
                Optional.of(new EconomicCounterparty(counterparty, Optional.of(counterpartyConsumer), Optional.empty())),
                new EconomicAmount(Currency.VALERITA, amount), Optional.empty(), FARMER,
                EconomicEventSourceType.TRANSACTION_LEDGER, Optional.empty());
    }

    private static EconomicEvent rejectedDecision(String sourceId, String instant, BankAccountId actor, ConsumerId consumer, int amount) {
        return event(sourceId, instant, EconomicEventType.OPERATION_REJECTED, EconomicEventCategory.OPERATIONAL_CONTROL,
                EconomicEventStatus.REJECTED, actor, consumer, Optional.empty(), new EconomicAmount(Currency.VALERITA, amount),
                Optional.empty(), SMITH, EconomicEventSourceType.OPERATIONAL_DECISION_JOURNAL, Optional.of("LIMIT_EXCEEDED"));
    }

    private static EconomicEvent exchangeEvent(String sourceId, String instant, BankAccountId actor, ConsumerId consumer,
                                               int sourceAmount, int targetAmount) {
        return event(sourceId, instant, EconomicEventType.CURRENCY_EXCHANGED, EconomicEventCategory.MONETARY,
                EconomicEventStatus.SUCCEEDED, actor, consumer, Optional.empty(), new EconomicAmount(Currency.SUELDO, sourceAmount),
                Optional.of(new EconomicAmount(Currency.VALERITA, targetAmount)), SMITH,
                EconomicEventSourceType.TRANSACTION_LEDGER, Optional.empty());
    }

    private static EconomicEvent event(String sourceId, String instant, EconomicEventType type, EconomicEventCategory category,
                                       EconomicEventStatus status, BankAccountId account, ConsumerId consumer,
                                       Optional<EconomicCounterparty> counterparty, EconomicAmount primary,
                                       Optional<EconomicAmount> secondary, Profession profession,
                                       EconomicEventSourceType sourceType, Optional<String> rejection) {
        EconomicEventSource source = new EconomicEventSource(sourceType, sourceId);
        return new EconomicEvent(source.eventId(), Instant.parse(instant), type, category, status,
                new EconomicActor(account, consumer), counterparty, Optional.of(primary), secondary,
                Optional.of(profession), Optional.empty(), rejection, source, Map.of());
    }

    private static EconomicEvent copyAt(EconomicEvent event, String instant) {
        return new EconomicEvent(event.id(), Instant.parse(instant), event.type(), event.category(), event.status(), event.actor(),
                event.counterparty(), event.primaryAmount(), event.secondaryAmount(), event.actorProfession(), event.productCategory(),
                event.rejectionReason(), event.source(), event.attributes());
    }

    private static void assertImmutable(List<?> values, String message) {
        try { ((List<Object>) values).add(new Object()); } catch (UnsupportedOperationException expected) { return; }
        throw new AssertionError(message);
    }
    private static void assertImmutableMap(Map<?, ?> values, String message) {
        try { ((Map<Object, Object>) values).put(new Object(), 1L); } catch (UnsupportedOperationException expected) { return; }
        throw new AssertionError(message);
    }
    private static void assertThrowsArgument(Runnable action, String message) {
        try { action.run(); } catch (IllegalArgumentException expected) { return; }
        throw new AssertionError(message + " did not throw IllegalArgumentException");
    }
    private static void assertThrowsState(Runnable action, String message) {
        try { action.run(); } catch (IllegalStateException expected) { return; }
        throw new AssertionError(message + " did not throw IllegalStateException");
    }
    private static void assertTrue(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void assertFalse(boolean condition, String message) { if (condition) throw new AssertionError(message); }
    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
    }
}
