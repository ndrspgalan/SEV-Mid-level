package consumerRegistry;

import accountHistory.*;
import banking.census.ProfessionCatalog;
import banking.census.ProfessionCensus;
import banking.census.ProfessionCensusSlot;
import banking.identity.*;

import java.time.Clock;
import java.util.*;

public final class ConsumerRegistry {
    private final Map<ConsumerId, Consumer> consumers = new LinkedHashMap<>();
    private final Map<String, Consumer> byInstitutionalId = new LinkedHashMap<>();
    private final Map<BankAccountId, Consumer> byAccountId = new LinkedHashMap<>();
    private final Map<String, Consumer> legacyAliases = new LinkedHashMap<>();
    private final ProfessionCatalog professionCatalog;
    private final ProfessionCensus professionCensus;
    private final AccountHistoryJournal accountHistoryJournal;
    private final Clock clock;

    public ConsumerRegistry() {
        this(ProfessionCatalog.valerianStandard(), new ProfessionCensus(), new InMemoryAccountHistoryJournal(), Clock.systemUTC());
    }
    public ConsumerRegistry(ProfessionCatalog professionCatalog, ProfessionCensus professionCensus) {
        this(professionCatalog, professionCensus, new InMemoryAccountHistoryJournal(), Clock.systemUTC());
    }
    public ConsumerRegistry(ProfessionCatalog professionCatalog, ProfessionCensus professionCensus,
                            AccountHistoryJournal accountHistoryJournal, Clock clock) {
        this.professionCatalog = Objects.requireNonNull(professionCatalog);
        this.professionCensus = Objects.requireNonNull(professionCensus);
        this.accountHistoryJournal = Objects.requireNonNull(accountHistoryJournal);
        this.clock = Objects.requireNonNull(clock);
    }

    public synchronized Consumer register(String legacyId, String name, String professionName) {
        Consumer consumer = register(name, professionName);
        String normalizedAlias = Objects.requireNonNull(legacyId).trim();
        if (!normalizedAlias.isEmpty()) legacyAliases.put(normalizedAlias, consumer);
        return consumer;
    }

    public synchronized Consumer register(String name, String professionName) {
        PersonName personName = new PersonName(name);
        Profession profession = professionCatalog.require(professionName);
        ensureUniqueName(personName);
        ConsumerId consumerId = ConsumerId.random();
        BankAccountId accountId = BankAccountId.random();
        ProfessionCensusSlot slot = professionCensus.allocate(profession, accountId);
        InstitutionalAccountId institutionalId = InstitutionalAccountId.compose(personName, profession, slot.position(), slot.reuseSequence());
        return storeAndRecord(new Consumer(consumerId, personName, new BankAccount(accountId, institutionalId, profession, slot.position(), slot.reuseSequence())));
    }

    public synchronized Consumer registerExact(String name, String professionName, int position, int reuse) {
        PersonName personName = new PersonName(name);
        Profession profession = professionCatalog.require(professionName);
        ensureUniqueName(personName);
        ConsumerId consumerId = ConsumerId.random();
        BankAccountId accountId = BankAccountId.random();
        ProfessionCensusSlot slot = professionCensus.reserveExact(profession, new CensusPosition(position), new ReuseSequence(reuse), accountId);
        InstitutionalAccountId institutionalId = InstitutionalAccountId.compose(personName, profession, slot.position(), slot.reuseSequence());
        return storeAndRecord(new Consumer(consumerId, personName, new BankAccount(accountId, institutionalId, profession, slot.position(), slot.reuseSequence())));
    }


    private Consumer storeAndRecord(Consumer consumer) {
        Consumer stored = store(consumer);
        accountHistoryJournal.append(new AccountHistoryEvent(
                AccountHistoryEventId.generate(), stored.getBankAccount().getBankAccountId(), stored.getStableConsumerId(),
                AccountHistoryEventType.ACCOUNT_REGISTERED, AccountHistoryEventStatus.COMPLETED, clock.instant(),
                null, stored.getBankAccount().getProfession(), null, stored.getBankAccount().getInstitutionalAccountId(),
                null, stored.getBankAccount().getHolderStatus(), null, "BANK_REGISTRATION"
        ));
        return stored;
    }

    private Consumer store(Consumer consumer) {
        String institutionalId = consumer.getConsumerId();
        if (byInstitutionalId.containsKey(institutionalId)) throw new IllegalStateException("institutional account id collision: " + institutionalId);
        consumers.put(consumer.getStableConsumerId(), consumer);
        byInstitutionalId.put(institutionalId, consumer);
        byAccountId.put(consumer.getBankAccount().getBankAccountId(), consumer);
        return consumer;
    }

    public synchronized void reindexInstitutionalId(Consumer consumer, String previousId) {
        byInstitutionalId.remove(previousId);
        String newId = consumer.getConsumerId();
        Consumer collision = byInstitutionalId.putIfAbsent(newId, consumer);
        if (collision != null && collision != consumer) throw new IllegalStateException("institutional account id collision: " + newId);
    }

    public Optional<Consumer> findById(String id) {
        Objects.requireNonNull(id, "id must not be null");
        String normalized = id.trim();
        Consumer byInstitutional = byInstitutionalId.get(normalized);
        if (byInstitutional != null) return Optional.of(byInstitutional);
        Consumer legacy = legacyAliases.get(normalized);
        if (legacy != null) return Optional.of(legacy);
        try { return Optional.ofNullable(consumers.get(ConsumerId.parse(normalized))); }
        catch (IllegalArgumentException ignored) { return Optional.empty(); }
    }
    public Optional<Consumer> findByAccountId(BankAccountId id) { return Optional.ofNullable(byAccountId.get(Objects.requireNonNull(id))); }
    public Collection<Consumer> all() { return List.copyOf(consumers.values()); }
    public ProfessionCatalog getProfessionCatalog() { return professionCatalog; }
    public ProfessionCensus getProfessionCensus() { return professionCensus; }
    public AccountHistoryJournal getAccountHistoryJournal() { return accountHistoryJournal; }
    public Clock getClock() { return clock; }

    private void ensureUniqueName(PersonName name) {
        boolean duplicate = consumers.values().stream().anyMatch(c -> c.getPersonName().value().equalsIgnoreCase(name.value()));
        if (duplicate) throw new IllegalArgumentException("person name is immutable and already registered: " + name);
    }
}
