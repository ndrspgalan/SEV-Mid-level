package tests;

import accountHistory.*;
import banking.identity.*;
import banking.lifecycle.AccountClosureReason;
import banking.lifecycle.AccountOperationalStatus;
import economicEvent.*;
import economicEvent.normalization.*;

import java.time.Instant;

/** Executable contract test for Mid M1.3 account-history normalization. */
public final class AccountHistoryEconomicEventNormalizerTest {
    private AccountHistoryEconomicEventNormalizerTest() {}

    public static void main(String[] args) {
        AccountHistoryEconomicEventNormalizer normalizer = new AccountHistoryEconomicEventNormalizer();
        Context context = context();

        normalizesAccountRegistration(normalizer, context);
        normalizesCompletedProfessionChange(normalizer, context);
        preservesRequestedProfessionWithoutCorruptingRejectedContext(normalizer, context);
        normalizesHolderLifecycle(normalizer, context);
        normalizesAccountLifecycle(normalizer, context);
        preservesClosureReason(normalizer, context);
        preservesDeterministicProvenance(normalizer, context);
        System.out.println("AccountHistoryEconomicEventNormalizerTest: PASSED");
    }

    private static void normalizesAccountRegistration(AccountHistoryEconomicEventNormalizer normalizer, Context c) {
        AccountHistoryEvent record = new AccountHistoryEvent(
                AccountHistoryEventId.generate(), c.accountId, c.consumerId,
                AccountHistoryEventType.ACCOUNT_REGISTERED, AccountHistoryEventStatus.COMPLETED, at(0),
                null, c.mendigo, null, c.oldInstitutionalId,
                null, HolderStatus.ASSIGNED, null, "BANK_REGISTRATION");

        EconomicEvent event = event(normalizer.normalize(record));
        check(event.type() == EconomicEventType.ACCOUNT_REGISTERED, "registration type");
        check(event.category() == EconomicEventCategory.INSTITUTIONAL, "registration category");
        check(event.status() == EconomicEventStatus.SUCCEEDED, "registration status");
        check(event.actorProfession().orElseThrow().equals(c.mendigo), "registration profession");
        check(event.actor().institutionalAccountId().orElseThrow().equals(c.oldInstitutionalId), "registration institutional id");
    }

    private static void normalizesCompletedProfessionChange(AccountHistoryEconomicEventNormalizer normalizer, Context c) {
        AccountHistoryEvent record = new AccountHistoryEvent(
                AccountHistoryEventId.generate(), c.accountId, c.consumerId,
                AccountHistoryEventType.PROFESSION_CHANGED, AccountHistoryEventStatus.COMPLETED, at(1),
                c.mendigo, c.mercader, c.oldInstitutionalId, c.newInstitutionalId,
                HolderStatus.ASSIGNED, HolderStatus.ASSIGNED, null, "PROFESSION_CHANGE");

        EconomicEvent event = event(normalizer.normalize(record));
        check(event.type() == EconomicEventType.PROFESSION_CHANGED, "profession-change type");
        check(event.actorProfession().orElseThrow().equals(c.mercader), "completed change uses resulting profession");
        check(event.actor().institutionalAccountId().orElseThrow().equals(c.newInstitutionalId), "completed change uses resulting id");
        check(event.attributes().get("previousProfession").equals(c.mendigo.toString()), "previous profession attribute");
        check(event.attributes().get("currentProfession").equals(c.mercader.toString()), "current profession attribute");
    }

    private static void preservesRequestedProfessionWithoutCorruptingRejectedContext(
            AccountHistoryEconomicEventNormalizer normalizer, Context c) {
        AccountHistoryEvent record = new AccountHistoryEvent(
                AccountHistoryEventId.generate(), c.accountId, c.consumerId,
                AccountHistoryEventType.PROFESSION_CHANGED, AccountHistoryEventStatus.REJECTED, at(2),
                c.mendigo, c.mercader, c.oldInstitutionalId, c.oldInstitutionalId,
                HolderStatus.ASSIGNED, HolderStatus.ASSIGNED, "CENSUS_SATURATED", "PROFESSION_CHANGE");

        EconomicEvent event = event(normalizer.normalize(record));
        check(event.status() == EconomicEventStatus.REJECTED, "rejected status");
        check(event.rejectionReason().orElseThrow().equals("CENSUS_SATURATED"), "rejection reason");
        check(event.actorProfession().orElseThrow().equals(c.mendigo), "rejected change keeps actual profession");
        check(event.attributes().get("currentProfession").equals(c.mercader.toString()), "requested profession retained as evidence");
    }

    private static void normalizesHolderLifecycle(AccountHistoryEconomicEventNormalizer normalizer, Context c) {
        AccountHistoryEvent released = new AccountHistoryEvent(
                AccountHistoryEventId.generate(), c.accountId, c.consumerId,
                AccountHistoryEventType.HOLDER_RELEASED, AccountHistoryEventStatus.COMPLETED, at(3),
                c.mendigo, c.mendigo, c.oldInstitutionalId, c.oldInstitutionalId,
                HolderStatus.ASSIGNED, HolderStatus.PENDING_NEW_HOLDER, null, "HOLDER_RELEASE");
        EconomicEvent releasedEvent = event(normalizer.normalize(released));
        check(releasedEvent.type() == EconomicEventType.HOLDER_RELEASED, "holder-release type");
        check(releasedEvent.attributes().get("currentHolderStatus").equals("PENDING_NEW_HOLDER"), "holder status retained");

        AccountHistoryEvent assigned = new AccountHistoryEvent(
                AccountHistoryEventId.generate(), c.accountId, c.consumerId,
                AccountHistoryEventType.HOLDER_ASSIGNED, AccountHistoryEventStatus.COMPLETED, at(4),
                c.mendigo, c.mendigo, c.oldInstitutionalId, c.oldInstitutionalId,
                HolderStatus.PENDING_NEW_HOLDER, HolderStatus.ASSIGNED, null, "HOLDER_ASSIGNMENT");
        check(event(normalizer.normalize(assigned)).type() == EconomicEventType.HOLDER_ASSIGNED, "holder-assignment type");
    }

    private static void normalizesAccountLifecycle(AccountHistoryEconomicEventNormalizer normalizer, Context c) {
        check(lifecycle(normalizer, c, AccountHistoryEventType.ACCOUNT_BLOCKED,
                AccountOperationalStatus.ACTIVE, AccountOperationalStatus.BLOCKED).type() == EconomicEventType.ACCOUNT_BLOCKED,
                "account-blocked type");
        check(lifecycle(normalizer, c, AccountHistoryEventType.ACCOUNT_UNBLOCKED,
                AccountOperationalStatus.BLOCKED, AccountOperationalStatus.ACTIVE).type() == EconomicEventType.ACCOUNT_UNBLOCKED,
                "account-unblocked type");
    }

    private static EconomicEvent lifecycle(AccountHistoryEconomicEventNormalizer normalizer, Context c,
                                           AccountHistoryEventType type,
                                           AccountOperationalStatus previous,
                                           AccountOperationalStatus current) {
        AccountHistoryEvent record = new AccountHistoryEvent(
                AccountHistoryEventId.generate(), c.accountId, c.consumerId,
                type, AccountHistoryEventStatus.COMPLETED, at(5),
                c.mendigo, c.mendigo, c.oldInstitutionalId, c.oldInstitutionalId,
                HolderStatus.ASSIGNED, HolderStatus.ASSIGNED,
                previous, current, null, null, "LIFECYCLE_REQUEST");
        EconomicEvent event = event(normalizer.normalize(record));
        check(event.category() == EconomicEventCategory.LIFECYCLE, "lifecycle category");
        check(event.attributes().get("previousOperationalStatus").equals(previous.name()), "previous operational status");
        check(event.attributes().get("currentOperationalStatus").equals(current.name()), "current operational status");
        return event;
    }

    private static void preservesClosureReason(AccountHistoryEconomicEventNormalizer normalizer, Context c) {
        AccountHistoryEvent record = new AccountHistoryEvent(
                AccountHistoryEventId.generate(), c.accountId, c.consumerId,
                AccountHistoryEventType.ACCOUNT_CLOSED, AccountHistoryEventStatus.COMPLETED, at(6),
                c.mendigo, c.mendigo, c.oldInstitutionalId, c.oldInstitutionalId,
                HolderStatus.ASSIGNED, HolderStatus.PENDING_NEW_HOLDER,
                AccountOperationalStatus.ACTIVE, AccountOperationalStatus.CLOSED,
                AccountClosureReason.VOLUNTARY, null, "CLOSE-001");
        EconomicEvent event = event(normalizer.normalize(record));
        check(event.type() == EconomicEventType.ACCOUNT_CLOSED, "account-closed type");
        check(event.attributes().get("closureReason").equals(AccountClosureReason.VOLUNTARY.name()), "closure reason");
    }

    private static void preservesDeterministicProvenance(AccountHistoryEconomicEventNormalizer normalizer, Context c) {
        AccountHistoryEventId id = AccountHistoryEventId.generate();
        AccountHistoryEvent record = new AccountHistoryEvent(
                id, c.accountId, c.consumerId,
                AccountHistoryEventType.ACCOUNT_BLOCKED, AccountHistoryEventStatus.REJECTED, at(7),
                c.mendigo, c.mendigo, c.oldInstitutionalId, c.oldInstitutionalId,
                HolderStatus.ASSIGNED, HolderStatus.ASSIGNED,
                AccountOperationalStatus.BLOCKED, AccountOperationalStatus.BLOCKED,
                null, "ALREADY_BLOCKED", "BLOCK-REQUEST-7");
        EconomicEvent first = event(normalizer.normalize(record));
        EconomicEvent second = event(normalizer.normalize(record));
        check(first.equals(second), "same history record must normalize deterministically");
        check(first.id().equals(first.source().eventId()), "event id derives from source");
        check(first.source().sourceId().equals(id.toString()), "source id");
        check(first.source().sourceReference().orElseThrow().equals("BLOCK-REQUEST-7"), "source reference");
    }

    private static Context context() {
        PersonName name = new PersonName("Kenan");
        Profession mendigo = new Profession("Mendigo", ProfessionCode.of("Mendigo"));
        Profession mercader = new Profession("Mercader", ProfessionCode.of("Mercader"));
        return new Context(
                BankAccountId.random(),
                ConsumerId.random(),
                mendigo,
                mercader,
                InstitutionalAccountId.compose(name, mendigo, new CensusPosition(1), new ReuseSequence(0)),
                InstitutionalAccountId.compose(name, mercader, new CensusPosition(2), new ReuseSequence(0)));
    }

    private static Instant at(int minute) {
        return Instant.parse("2026-07-20T10:" + String.format("%02d", minute) + ":00Z");
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

    private record Context(BankAccountId accountId, ConsumerId consumerId,
                           Profession mendigo, Profession mercader,
                           InstitutionalAccountId oldInstitutionalId,
                           InstitutionalAccountId newInstitutionalId) {}
}
