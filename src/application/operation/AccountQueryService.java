package application.operation;

import accountHistory.AccountHistoryEvent;
import accountHistory.AccountHistoryEventStatus;
import accountHistory.AccountHistoryEventType;
import coinProperties.Currency;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;

import java.util.EnumMap;
import java.util.Objects;
import java.util.Optional;

public final class AccountQueryService {

    private final ConsumerRegistry consumerRegistry;

    public AccountQueryService(ConsumerRegistry consumerRegistry) {
        this.consumerRegistry = Objects.requireNonNull(consumerRegistry);
    }

    public Optional<AccountSnapshot> findAccount(String consumerId) {
        return consumerRegistry.findById(consumerId).map(this::toSnapshot);
    }

    private AccountSnapshot toSnapshot(Consumer consumer) {
        EnumMap<Currency, Integer> balances = new EnumMap<>(Currency.class);
        for (Currency currency : Currency.values()) {
            balances.put(
                    currency,
                    consumer.getBankAccount().getBalance(currency)
            );
        }
        java.util.List<AccountHistoryEvent> history = consumerRegistry.getAccountHistoryJournal().findAll().stream()
                .filter(event -> event.bankAccountId().equals(consumer.getBankAccount().getBankAccountId()))
                .toList();
        long professionChanges = history.stream().filter(event -> event.type() == AccountHistoryEventType.PROFESSION_CHANGED)
                .filter(event -> event.status() == AccountHistoryEventStatus.COMPLETED).count();
        long holderChanges = history.stream().filter(event -> event.type() == AccountHistoryEventType.HOLDER_RELEASED
                || event.type() == AccountHistoryEventType.HOLDER_ASSIGNED)
                .filter(event -> event.status() == AccountHistoryEventStatus.COMPLETED).count();
        java.time.Instant lastModified = history.stream().map(AccountHistoryEvent::occurredAt)
                .max(java.time.Instant::compareTo).orElse(null);
        return new AccountSnapshot(
                consumer.getStableConsumerId().toString(),
                consumer.getBankAccount().getBankAccountId().toString(),
                consumer.getBankAccount().getInstitutionalAccountId().toString(),
                consumer.getName(),
                consumer.getProfession(),
                consumer.getBankAccount().getCensusPosition().value(),
                consumer.getBankAccount().getReuseSequence().value(),
                consumer.getBankAccount().getHolderStatus(),
                consumer.getBankAccount().getOperationalStatus(),
                balances, professionChanges, holderChanges, lastModified
        );
    }
}
