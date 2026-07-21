package tests;

import application.operation.TransactionQueryService;
import application.operation.TransactionStatisticsService;
import application.view.TransactionDetailView;
import application.view.TransactionStatistics;
import application.view.TransactionSummary;
import coinProperties.Currency;
import transaction.*;
import transaction.query.PageRequest;
import transaction.query.SortDirection;
import transaction.query.TransactionPage;
import transaction.query.TransactionQuery;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class TransactionQueryServiceTest {

    private TransactionQueryServiceTest() {
    }

    public static void main(String[] args) {
        InMemoryTransactionLedger ledger = new InMemoryTransactionLedger();
        TransactionRecord first = exchangeRecord(
                "00000000-0000-0000-0000-000000000001",
                "2026-07-19T10:00:00Z",
                "consumer-a",
                TransactionStatus.COMPLETED
        );
        TransactionRecord second = purchaseRecord(
                "00000000-0000-0000-0000-000000000002",
                "2026-07-19T11:00:00Z",
                "consumer-a",
                "consumer-b",
                TransactionStatus.REJECTED
        );
        TransactionRecord third = exchangeRecord(
                "00000000-0000-0000-0000-000000000003",
                "2026-07-19T12:00:00Z",
                "consumer-c",
                TransactionStatus.COMPLETED
        );
        ledger.append(first);
        ledger.append(second);
        ledger.append(third);

        TransactionQueryService service = new TransactionQueryService(ledger);
        TransactionStatisticsService statisticsService =
                new TransactionStatisticsService(service);

        testNewestFirstAndPagination(service);
        testCombinedFilters(service);
        testTimeBoundaries(service);
        testDetailProjection(service, second.id());
        testStatistics(statisticsService);
        testEmptyOutOfRangePage(service);
        testValidation();

        System.out.println("TransactionQueryServiceTest: OK");
    }

    private static void testNewestFirstAndPagination(
            TransactionQueryService service
    ) {
        TransactionPage<TransactionSummary> firstPage = service.search(
                TransactionQuery.all(new PageRequest(0, 2))
        );
        assertEquals(3L, firstPage.totalElements(), "total elements");
        assertEquals(2, firstPage.totalPages(), "total pages");
        assertEquals(2, firstPage.content().size(), "first page size");
        assertEquals(
                "00000000-0000-0000-0000-000000000003",
                firstPage.content().get(0).id().toString(),
                "newest record first"
        );
        assertTrue(firstPage.hasNext(), "first page must have next");
        assertFalse(firstPage.hasPrevious(), "first page must not have previous");

        boolean immutable = false;
        try {
            firstPage.content().add(firstPage.content().get(0));
        } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        assertTrue(immutable, "page content must be immutable");
    }

    private static void testCombinedFilters(TransactionQueryService service) {
        TransactionQuery query = new TransactionQuery(
                Optional.of(TransactionType.PURCHASE),
                Optional.of(TransactionStatus.REJECTED),
                Optional.of("consumer-b"),
                Optional.empty(),
                Optional.empty(),
                SortDirection.OLDEST_FIRST,
                PageRequest.firstPage(10)
        );
        TransactionPage<TransactionSummary> page = service.search(query);
        assertEquals(1L, page.totalElements(), "combined filter count");
        assertEquals(TransactionType.PURCHASE, page.content().get(0).type(), "type");
        assertEquals(TransactionStatus.REJECTED, page.content().get(0).status(), "status");
    }

    private static void testTimeBoundaries(TransactionQueryService service) {
        TransactionQuery query = new TransactionQuery(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(Instant.parse("2026-07-19T11:00:00Z")),
                Optional.of(Instant.parse("2026-07-19T12:00:00Z")),
                SortDirection.OLDEST_FIRST,
                PageRequest.firstPage(10)
        );
        TransactionPage<TransactionSummary> page = service.search(query);
        assertEquals(1L, page.totalElements(), "inclusive/exclusive time range");
        assertEquals(
                "00000000-0000-0000-0000-000000000002",
                page.content().get(0).id().toString(),
                "time range record"
        );
    }

    private static void testDetailProjection(
            TransactionQueryService service,
            TransactionId id
    ) {
        TransactionDetailView detail = service.findById(id).orElseThrow();
        assertEquals(2, detail.participantIds().size(), "purchase participants");
        assertEquals("consumer-a", detail.attributes().get("buyerId"), "buyer attribute");
        assertEquals("consumer-b", detail.attributes().get("sellerId"), "seller attribute");
        assertFalse(detail.attributes().containsKey("buyerBalanceAfter"), "rejected balance absent");
    }

    private static void testStatistics(
            TransactionStatisticsService statisticsService
    ) {
        TransactionStatistics statistics = statisticsService.calculate(
                TransactionQuery.all(PageRequest.firstPage(100))
        );
        assertEquals(3L, statistics.total(), "statistics total");
        assertEquals(2L, statistics.count(TransactionType.EXCHANGE), "exchange count");
        assertEquals(1L, statistics.count(TransactionType.PURCHASE), "purchase count");
        assertEquals(2L, statistics.count(TransactionStatus.COMPLETED), "completed count");
        assertEquals(1L, statistics.count(TransactionStatus.REJECTED), "rejected count");
    }

    private static void testEmptyOutOfRangePage(
            TransactionQueryService service
    ) {
        TransactionQuery query = new TransactionQuery(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                SortDirection.NEWEST_FIRST,
                new PageRequest(5, 2)
        );
        TransactionPage<TransactionSummary> page = service.search(query);
        assertTrue(page.isEmpty(), "out-of-range page must be empty");
        assertEquals(3L, page.totalElements(), "out-of-range total preserved");
    }

    private static void testValidation() {
        assertThrows(
                () -> new PageRequest(-1, 10),
                "negative page number"
        );
        assertThrows(
                () -> new PageRequest(0, 101),
                "oversized page"
        );
        assertThrows(
                () -> new TransactionQuery(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        Optional.of(Instant.parse("2026-07-19T12:00:00Z")),
                        Optional.of(Instant.parse("2026-07-19T11:00:00Z")),
                        SortDirection.NEWEST_FIRST,
                        PageRequest.firstPage(10)
                ),
                "invalid interval"
        );
    }

    private static TransactionRecord exchangeRecord(
            String id,
            String instant,
            String consumer,
            TransactionStatus status
    ) {
        boolean completed = status == TransactionStatus.COMPLETED;
        return new TransactionRecord(
                id(id),
                Instant.parse(instant),
                TransactionType.EXCHANGE,
                status,
                new ExchangeTransactionDetails(
                        consumer,
                        Currency.SUELDO,
                        Currency.VALERITA,
                        1,
                        completed ? Optional.of(100) : Optional.empty(),
                        completed ? Optional.of(10) : Optional.empty(),
                        completed ? Optional.of(9) : Optional.empty(),
                        completed ? Optional.of(0) : Optional.empty(),
                        completed ? Optional.of(100) : Optional.empty(),
                        completed ? Optional.empty() : Optional.of("REJECTED")
                )
        );
    }

    private static TransactionRecord purchaseRecord(
            String id,
            String instant,
            String buyer,
            String seller,
            TransactionStatus status
    ) {
        boolean completed = status == TransactionStatus.COMPLETED;
        return new TransactionRecord(
                id(id),
                Instant.parse(instant),
                TransactionType.PURCHASE,
                status,
                new PurchaseTransactionDetails(
                        buyer,
                        seller,
                        "FOOD-001",
                        Optional.of(Currency.VALERITA),
                        Optional.of(20),
                        Optional.of(10),
                        completed ? Optional.of(0) : Optional.empty(),
                        Optional.of(0),
                        completed ? Optional.of(20) : Optional.empty(),
                        completed ? Optional.empty() : Optional.of("INSUFFICIENT_BALANCE")
                )
        );
    }

    private static TransactionId id(String value) {
        return new TransactionId(UUID.fromString(value));
    }

    private static void assertThrows(Runnable action, String message) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError(message + " did not throw");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(
                    message + ": expected=" + expected + ", actual=" + actual
            );
        }
    }
}
