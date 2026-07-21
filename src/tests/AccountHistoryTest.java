package tests;

import accountHistory.*;
import application.account.AccountHolderService;
import application.account.ProfessionChangeService;
import application.history.AccountHistoryQueryService;
import application.history.AccountHistoryStatisticsService;
import banking.census.ProfessionCatalog;
import banking.census.ProfessionCensus;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class AccountHistoryTest {
    public static void main(String[] args) {
        registrationAndProfessionChangesAreRecorded();
        rejectedAttemptsAreRecordedWithoutChangingIdentity();
        holderReleaseIsRecorded();
        querySupportsFilteringOrderingAndPaging();
        statisticsPreserveTemporalMeaning();
        System.out.println("AccountHistoryTest: OK");
    }

    private static ConsumerRegistry registryAt(String instant) {
        return new ConsumerRegistry(ProfessionCatalog.valerianStandard(), new ProfessionCensus(),
                new InMemoryAccountHistoryJournal(), Clock.fixed(Instant.parse(instant), ZoneOffset.UTC));
    }

    private static void registrationAndProfessionChangesAreRecorded() {
        ConsumerRegistry registry = registryAt("1456-01-30T10:00:00Z");
        Consumer person = registry.register("Álvaro", "Carpintero");
        String oldId = person.getConsumerId();
        new ProfessionChangeService(registry).change(oldId, "Jornalero");
        var events = registry.getAccountHistoryJournal().findAll();
        check(events.size() == 2, "registration plus profession change");
        AccountHistoryEvent change = events.get(1);
        check(change.type() == AccountHistoryEventType.PROFESSION_CHANGED, "profession event type");
        check(change.status() == AccountHistoryEventStatus.COMPLETED, "profession event completed");
        check(change.previousProfession().orElseThrow().name().equals("Carpintero"), "previous profession retained");
        check(change.currentProfession().orElseThrow().name().equals("Jornalero"), "new profession retained");
        check(change.previousInstitutionalId().orElseThrow().toString().equals(oldId), "old identity retained");
        check(change.currentInstitutionalId().orElseThrow().toString().equals(person.getConsumerId()), "new identity retained");
    }

    private static void rejectedAttemptsAreRecordedWithoutChangingIdentity() {
        ConsumerRegistry registry = registryAt("1456-01-30T10:00:00Z");
        Consumer person = registry.register("María Luisa", "Carpintero");
        String before = person.getConsumerId();
        new ProfessionChangeService(registry).change(before, "Alquimista");
        AccountHistoryEvent event = registry.getAccountHistoryJournal().findAll().get(1);
        check(event.status() == AccountHistoryEventStatus.REJECTED, "rejection recorded");
        check(event.rejectionReason().orElseThrow().equals("PROFESSION_NOT_ACCEPTED"), "typed rejection retained");
        check(person.getConsumerId().equals(before), "rejected event does not mutate identity");
    }

    private static void holderReleaseIsRecorded() {
        ConsumerRegistry registry = registryAt("1456-01-30T10:00:00Z");
        Consumer person = registry.register("Juan-Pablo", "Jornalero");
        var result = new AccountHolderService(registry).releaseHolder(person.getConsumerId());
        check(result.completed(), "zero-balance account holder released");
        AccountHistoryEvent event = registry.getAccountHistoryJournal().findAll().get(1);
        check(event.type() == AccountHistoryEventType.HOLDER_RELEASED, "holder release type");
        check(event.previousHolderStatus().orElseThrow() == banking.identity.HolderStatus.ASSIGNED, "previous holder state");
        check(event.currentHolderStatus().orElseThrow() == banking.identity.HolderStatus.PENDING_NEW_HOLDER, "new holder state");
    }

    private static void querySupportsFilteringOrderingAndPaging() {
        ConsumerRegistry registry = registryAt("1456-01-30T10:00:00Z");
        Consumer person = registry.register("Álvaro", "Carpintero");
        ProfessionChangeService service = new ProfessionChangeService(registry);
        service.change(person.getConsumerId(), "Jornalero");
        service.change(person.getConsumerId(), "Mercader");
        AccountHistoryQueryService queryService = new AccountHistoryQueryService(registry.getAccountHistoryJournal());
        AccountHistoryQuery query = new AccountHistoryQuery(person.getBankAccount().getBankAccountId(), null,
                AccountHistoryEventType.PROFESSION_CHANGED, AccountHistoryEventStatus.COMPLETED,
                Instant.parse("1456-01-30T00:00:00Z"), Instant.parse("1456-01-31T00:00:00Z"),
                AccountHistorySortDirection.NEWEST_FIRST);
        AccountHistoryPage page = queryService.search(query, new AccountHistoryPageRequest(0, 1));
        check(page.totalElements() == 2, "combined filters");
        check(page.content().size() == 1 && page.hasNext(), "paging");
        try {
            page.content().add(page.content().get(0));
            throw new AssertionError("page content must be immutable");
        } catch (UnsupportedOperationException expected) { }
    }

    private static void statisticsPreserveTemporalMeaning() {
        InMemoryAccountHistoryJournal journal = new InMemoryAccountHistoryJournal();
        ConsumerRegistry registry = new ConsumerRegistry(ProfessionCatalog.valerianStandard(), new ProfessionCensus(), journal,
                Clock.fixed(Instant.parse("1456-01-01T00:00:00Z"), ZoneOffset.UTC));
        Consumer person = registry.register("Álvaro", "Carpintero");
        ProfessionChangeService first = new ProfessionChangeService(registry, journal,
                Clock.fixed(Instant.parse("1456-01-11T00:00:00Z"), ZoneOffset.UTC));
        first.change(person.getConsumerId(), "Jornalero");
        ProfessionChangeService second = new ProfessionChangeService(registry, journal,
                Clock.fixed(Instant.parse("1456-01-21T00:00:00Z"), ZoneOffset.UTC));
        second.change(person.getConsumerId(), "Mercader");
        AccountHistoryQueryService queryService = new AccountHistoryQueryService(journal);
        AccountHistoryStatistics stats = new AccountHistoryStatisticsService(queryService).calculate(
                new AccountHistoryQuery(person.getBankAccount().getBankAccountId(), null, null, null, null, null,
                        AccountHistorySortDirection.OLDEST_FIRST));
        check(stats.professionChanges() == 2, "profession change count");
        check(stats.averageProfessionChangeInterval().orElseThrow().toDays() == 10, "average interval captures temporal density");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
