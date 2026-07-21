package tests;

import application.operation.TransactionQueryService;
import application.operation.TransferOperationResult;
import application.operation.TransferOperationService;
import application.view.TransactionDetailView;
import coinProperties.Currency;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;
import transaction.InMemoryTransactionLedger;
import transaction.TransactionLedger;
import transaction.TransactionStatus;
import transaction.TransactionType;
import transfer.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

public final class TransferOperationServiceTest {

    private static final Instant FIXED_TIME =
            Instant.parse("2026-07-19T18:00:00Z");

    private TransferOperationServiceTest() {
    }

    public static void main(String[] args) {
        testCompletedTransferAndConservation();
        testRejectedTransfersDoNotMutateBalances();
        testIdempotentReplayDoesNotMoveMoneyTwice();
        testIdempotencyConflict();
        testQueryIntegration();
        System.out.println("TransferOperationServiceTest: OK");
    }

    private static void testCompletedTransferAndConservation() {
        Fixture fixture = fixture();
        TransferRequest request = request(
                "00000000-0000-0000-0000-000000000101",
                "source",
                "destination",
                125,
                "PAYMENT-001"
        );

        int totalBefore = fixture.totalBalance();
        TransferOperationResult result = fixture.service.transfer(request);

        assertTrue(result.isCompleted(), "transfer must complete");
        assertFalse(result.isIdempotentReplay(), "first execution is not replay");
        assertEquals(500, result.getSourceBalanceBefore().orElseThrow(), "source before");
        assertEquals(375, result.getSourceBalanceAfter().orElseThrow(), "source after");
        assertEquals(40, result.getDestinationBalanceBefore().orElseThrow(), "destination before");
        assertEquals(165, result.getDestinationBalanceAfter().orElseThrow(), "destination after");
        assertEquals(totalBefore, fixture.totalBalance(), "money must be conserved");
        assertEquals(1, fixture.ledger.findAll().size(), "one ledger record");
        assertEquals(TransactionStatus.COMPLETED, fixture.ledger.findAll().get(0).status(), "completed status");
        assertEquals(FIXED_TIME, result.getOccurredAt(), "fixed clock");
    }

    private static void testRejectedTransfersDoNotMutateBalances() {
        assertRejectedWithoutMutation(
                request("00000000-0000-0000-0000-000000000102", "missing", "destination", 10, "MISSING-SOURCE"),
                TransferRejectionReason.SOURCE_CONSUMER_NOT_FOUND
        );
        assertRejectedWithoutMutation(
                request("00000000-0000-0000-0000-000000000103", "source", "missing", 10, "MISSING-DESTINATION"),
                TransferRejectionReason.DESTINATION_CONSUMER_NOT_FOUND
        );
        assertRejectedWithoutMutation(
                request("00000000-0000-0000-0000-000000000104", "source", "source", 10, "SAME"),
                TransferRejectionReason.SAME_SOURCE_AND_DESTINATION_ACCOUNT
        );
        assertRejectedWithoutMutation(
                request("00000000-0000-0000-0000-000000000105", "source", "destination", 0, "ZERO"),
                TransferRejectionReason.NON_POSITIVE_QUANTITY
        );
        assertRejectedWithoutMutation(
                request("00000000-0000-0000-0000-000000000106", "source", "destination", -1, "NEGATIVE"),
                TransferRejectionReason.NON_POSITIVE_QUANTITY
        );
        assertRejectedWithoutMutation(
                request("00000000-0000-0000-0000-000000000107", "source", "destination", 501, "INSUFFICIENT"),
                TransferRejectionReason.INSUFFICIENT_BALANCE
        );
    }

    private static void assertRejectedWithoutMutation(
            TransferRequest request,
            TransferRejectionReason expectedReason
    ) {
        Fixture fixture = fixture();
        int sourceBefore = fixture.source.getBankAccount().getBalance(Currency.SUELDO);
        int destinationBefore = fixture.destination.getBankAccount().getBalance(Currency.SUELDO);
        int totalBefore = fixture.totalBalance();

        TransferOperationResult result = fixture.service.transfer(request);

        assertTrue(result.isRejected(), "transfer must be rejected");
        assertEquals(expectedReason, result.getRejectionReason().orElseThrow(), "rejection reason");
        assertEquals(sourceBefore, fixture.source.getBankAccount().getBalance(Currency.SUELDO), "source unchanged");
        assertEquals(destinationBefore, fixture.destination.getBankAccount().getBalance(Currency.SUELDO), "destination unchanged");
        assertEquals(totalBefore, fixture.totalBalance(), "total unchanged");
        assertEquals(TransactionStatus.REJECTED, fixture.ledger.findAll().get(0).status(), "rejected status");
    }

    private static void testIdempotentReplayDoesNotMoveMoneyTwice() {
        Fixture fixture = fixture();
        TransferRequest request = request(
                "00000000-0000-0000-0000-000000000108",
                "source",
                "destination",
                100,
                "RETRYABLE"
        );

        TransferOperationResult first = fixture.service.transfer(request);
        int sourceAfterFirst = fixture.source.getBankAccount().getBalance(Currency.SUELDO);
        int destinationAfterFirst = fixture.destination.getBankAccount().getBalance(Currency.SUELDO);
        TransferOperationResult replay = fixture.service.transfer(request);

        assertTrue(first.isCompleted(), "first transfer completed");
        assertTrue(replay.isCompleted(), "replay preserves completed result");
        assertTrue(replay.isIdempotentReplay(), "second response is replay");
        assertEquals(first.getTransactionId(), replay.getTransactionId(), "same transaction id");
        assertEquals(sourceAfterFirst, fixture.source.getBankAccount().getBalance(Currency.SUELDO), "source not debited twice");
        assertEquals(destinationAfterFirst, fixture.destination.getBankAccount().getBalance(Currency.SUELDO), "destination not credited twice");
        assertEquals(1, fixture.ledger.findAll().size(), "replay creates no second transaction");
    }

    private static void testIdempotencyConflict() {
        Fixture fixture = fixture();
        TransferRequest first = request(
                "00000000-0000-0000-0000-000000000109",
                "source",
                "destination",
                50,
                "ORIGINAL"
        );
        fixture.service.transfer(first);
        int sourceAfterFirst = fixture.source.getBankAccount().getBalance(Currency.SUELDO);

        TransferRequest conflicting = request(
                "00000000-0000-0000-0000-000000000109",
                "source",
                "destination",
                60,
                "CHANGED"
        );
        TransferOperationResult conflict = fixture.service.transfer(conflicting);

        assertTrue(conflict.isIdempotencyConflict(), "must report idempotency conflict");
        assertEquals(sourceAfterFirst, fixture.source.getBankAccount().getBalance(Currency.SUELDO), "conflict does not debit");
        assertEquals(1, fixture.ledger.findAll().size(), "conflict is not a second economic transaction");
    }

    private static void testQueryIntegration() {
        Fixture fixture = fixture();
        TransferOperationResult result = fixture.service.transfer(request(
                "00000000-0000-0000-0000-000000000110",
                "source",
                "destination",
                75,
                "QUERY"
        ));

        TransactionDetailView detail = new TransactionQueryService(fixture.ledger)
                .findById(result.getTransactionId())
                .orElseThrow();
        assertEquals(TransactionType.TRANSFER, detail.type(), "query type");
        assertTrue(detail.participantIds().contains("source"), "source participant");
        assertTrue(detail.participantIds().contains("destination"), "destination participant");
        assertEquals("75", detail.attributes().get("quantity"), "quantity attribute");
        assertEquals("QUERY", detail.attributes().get("reference"), "reference attribute");
    }

    private static Fixture fixture() {
        ConsumerRegistry consumers = new ConsumerRegistry();
        Consumer source = consumers.register("source", "Source", "Trader");
        Consumer destination = consumers.register("destination", "Destination", "Merchant");
        source.getBankAccount().deposit(Currency.SUELDO, 500);
        destination.getBankAccount().deposit(Currency.SUELDO, 40);
        TransactionLedger ledger = new InMemoryTransactionLedger();
        TransferOperationService service = new TransferOperationService(
                consumers,
                new ImplementedTransferPolicy(),
                new InMemoryTransferRequestRegistry(),
                ledger,
                Clock.fixed(FIXED_TIME, ZoneOffset.UTC)
        );
        return new Fixture(consumers, source, destination, ledger, service);
    }

    private static TransferRequest request(
            String id,
            String source,
            String destination,
            int quantity,
            String reference
    ) {
        return new TransferRequest(
                new TransferRequestId(UUID.fromString(id)),
                source,
                destination,
                Currency.SUELDO,
                quantity,
                reference
        );
    }

    private record Fixture(
            ConsumerRegistry consumers,
            Consumer source,
            Consumer destination,
            TransactionLedger ledger,
            TransferOperationService service
    ) {
        int totalBalance() {
            return source.getBankAccount().getBalance(Currency.SUELDO)
                    + destination.getBankAccount().getBalance(Currency.SUELDO);
        }
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
