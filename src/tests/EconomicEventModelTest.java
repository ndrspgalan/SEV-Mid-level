package tests;

import banking.identity.BankAccountId;
import banking.identity.ConsumerId;
import coinProperties.Currency;
import economicEvent.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Executable contract test for Mid M1.1 canonical economic-event model. */
public final class EconomicEventModelTest {
    private EconomicEventModelTest() {}

    public static void main(String[] args) {
        deterministicIdentityAndImmutability();
        transferRequiresCounterparty();
        rejectedEventRequiresReason();
        exchangeRequiresTwoDifferentCurrencies();
        categoryMustMatchType();
        System.out.println("EconomicEventModelTest: PASSED");
    }

    private static void deterministicIdentityAndImmutability() {
        EconomicEventSource source = new EconomicEventSource(EconomicEventSourceType.TRANSACTION_LEDGER, "tx-41", "TransferTransactionDetails");
        EconomicActor actor = new EconomicActor(BankAccountId.random(), ConsumerId.random());
        EconomicCounterparty counterparty = new EconomicCounterparty(BankAccountId.random());
        Map<String, String> attributes = new LinkedHashMap<>();
        attributes.put("route", "direct");

        EconomicEvent event = new EconomicEvent(
                source.eventId(), Instant.parse("2026-07-20T10:00:00Z"),
                EconomicEventType.FUNDS_TRANSFERRED, EconomicEventCategory.TRANSFER,
                EconomicEventStatus.SUCCEEDED, actor, Optional.of(counterparty),
                Optional.of(new EconomicAmount(Currency.VALERITA, 10)), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), source, attributes);

        check(event.id().equals(EconomicEventId.fromSource(EconomicEventSourceType.TRANSACTION_LEDGER, "tx-41")),
                "event identity must be deterministic");
        attributes.put("late", "mutation");
        check(!event.attributes().containsKey("late"), "attributes must be defensively copied");
        expect(UnsupportedOperationException.class, () -> event.attributes().put("x", "y"));
    }

    private static void transferRequiresCounterparty() {
        EconomicEventSource source = new EconomicEventSource(EconomicEventSourceType.TRANSACTION_LEDGER, "tx-42");
        expect(IllegalArgumentException.class, () -> new EconomicEvent(
                source.eventId(), Instant.now(), EconomicEventType.FUNDS_TRANSFERRED,
                EconomicEventCategory.TRANSFER, EconomicEventStatus.SUCCEEDED,
                new EconomicActor(BankAccountId.random(), ConsumerId.random()), Optional.empty(),
                Optional.of(new EconomicAmount(Currency.SUELDO, 2)), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), source, Map.of()));
    }

    private static void rejectedEventRequiresReason() {
        EconomicEventSource source = new EconomicEventSource(EconomicEventSourceType.OPERATIONAL_DECISION_JOURNAL, "decision-1");
        expect(IllegalArgumentException.class, () -> new EconomicEvent(
                source.eventId(), Instant.now(), EconomicEventType.OPERATION_REJECTED,
                EconomicEventCategory.OPERATIONAL_CONTROL, EconomicEventStatus.REJECTED,
                new EconomicActor(BankAccountId.random(), ConsumerId.random()), Optional.empty(),
                Optional.of(new EconomicAmount(Currency.BERYLARE, 4)), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), source, Map.of()));
    }

    private static void exchangeRequiresTwoDifferentCurrencies() {
        EconomicEventSource source = new EconomicEventSource(EconomicEventSourceType.TRANSACTION_LEDGER, "tx-43");
        expect(IllegalArgumentException.class, () -> new EconomicEvent(
                source.eventId(), Instant.now(), EconomicEventType.CURRENCY_EXCHANGED,
                EconomicEventCategory.MONETARY, EconomicEventStatus.SUCCEEDED,
                new EconomicActor(BankAccountId.random(), ConsumerId.random()), Optional.empty(),
                Optional.of(new EconomicAmount(Currency.VALERITA, 10)),
                Optional.of(new EconomicAmount(Currency.VALERITA, 1)),
                Optional.empty(), Optional.empty(), Optional.empty(), source, Map.of()));
    }

    private static void categoryMustMatchType() {
        EconomicEventSource source = new EconomicEventSource(EconomicEventSourceType.ACCOUNT_HISTORY_JOURNAL, "history-1");
        expect(IllegalArgumentException.class, () -> new EconomicEvent(
                source.eventId(), Instant.now(), EconomicEventType.ACCOUNT_BLOCKED,
                EconomicEventCategory.INSTITUTIONAL, EconomicEventStatus.SUCCEEDED,
                new EconomicActor(BankAccountId.random(), ConsumerId.random()), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), source, Map.of()));
    }

    private static void expect(Class<? extends Throwable> expected, Runnable operation) {
        try {
            operation.run();
            throw new AssertionError("expected " + expected.getSimpleName());
        } catch (Throwable actual) {
            if (!expected.isInstance(actual)) throw new AssertionError("expected " + expected.getSimpleName() + " but got " + actual, actual);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
