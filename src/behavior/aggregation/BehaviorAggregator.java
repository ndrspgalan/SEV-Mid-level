package behavior.aggregation;

import banking.identity.BankAccountId;
import banking.identity.ConsumerId;
import behavior.profile.*;
import coinProperties.Currency;
import consumableRegistry.ConsumableCategory;
import economicEvent.*;

import java.time.Instant;
import java.util.*;

/** Deterministically aggregates canonical economic events into one descriptive profile per consumer. */
public final class BehaviorAggregator {

    public List<BehaviorProfile> aggregate(Iterable<EconomicEvent> events) {
        Objects.requireNonNull(events, "events must not be null");
        Map<ConsumerId, MutableProfile> byConsumer = new TreeMap<>(Comparator.comparing(ConsumerId::toString));
        for (EconomicEvent event : events) {
            Objects.requireNonNull(event, "economic event must not be null");
            byConsumer.computeIfAbsent(event.actor().consumerId(), MutableProfile::new).accept(event);
        }
        List<BehaviorProfile> result = new ArrayList<>();
        byConsumer.values().forEach(value -> result.add(value.freeze()));
        return List.copyOf(result);
    }

    private static final class MutableProfile {
        private final ConsumerId consumerId;
        private final Set<BankAccountId> accounts = new LinkedHashSet<>();
        private Instant first;
        private Instant last;
        private long total;
        private long succeeded;
        private long rejected;
        private final EnumMap<EconomicEventType, Long> byType = new EnumMap<>(EconomicEventType.class);
        private final EnumMap<Currency, Integer> volume = new EnumMap<>(Currency.class);
        private final Set<BankAccountId> counterparties = new LinkedHashSet<>();
        private final Map<String, MutableConsumable> consumables = new LinkedHashMap<>();

        private MutableProfile(ConsumerId consumerId) { this.consumerId = consumerId; }

        private void accept(EconomicEvent event) {
            total++;
            if (event.rejected()) rejected++; else succeeded++;
            accounts.add(event.actor().accountId());
            first = first == null || event.occurredAt().isBefore(first) ? event.occurredAt() : first;
            last = last == null || event.occurredAt().isAfter(last) ? event.occurredAt() : last;
            byType.merge(event.type(), 1L, Long::sum);
            event.counterparty().ifPresent(value -> counterparties.add(value.accountId()));
            if (!event.rejected()) {
                event.primaryAmount().ifPresent(amount -> volume.merge(amount.currency(), amount.amount(), Math::addExact));
                event.secondaryAmount().ifPresent(amount -> volume.merge(amount.currency(), amount.amount(), Math::addExact));
                if (event.type() == EconomicEventType.PURCHASE_EXECUTED) acceptPurchase(event);
            }
        }

        private void acceptPurchase(EconomicEvent event) {
            Map<String, String> a = event.attributes();
            String id = required(a, "consumableId", event);
            String name = a.getOrDefault("consumableName", id);
            String categoryText = required(a, "consumableCategory", event);
            ConsumableCategory category;
            try { category = ConsumableCategory.valueOf(categoryText); }
            catch (IllegalArgumentException ex) { throw invalid(event, "unknown consumable category: " + categoryText); }
            int quantity = positiveInt(required(a, "quantity", event), "quantity", event);
            EconomicAmount totalAmount = event.primaryAmount().orElseThrow(() -> invalid(event, "purchase lacks primary amount"));
            int unitPrice = a.containsKey("unitPrice")
                    ? nonNegativeInt(a.get("unitPrice"), "unitPrice", event)
                    : totalAmount.amount() / quantity;
            if (Math.multiplyExact(unitPrice, quantity) != totalAmount.amount()) {
                throw invalid(event, "unitPrice * quantity differs from purchase total");
            }
            consumables.computeIfAbsent(id, ignored -> new MutableConsumable(id, name, category))
                    .accept(event.occurredAt(), quantity, unitPrice, totalAmount);
        }

        private BehaviorProfile freeze() {
            Map<String, ConsumableBehaviorProfile> frozenConsumables = new LinkedHashMap<>();
            consumables.values().forEach(value -> frozenConsumables.put(value.id, value.freeze()));
            Map<ConsumableCategory, CategoryBehaviorProfile> categories = aggregateCategories(frozenConsumables.values());
            return new BehaviorProfile(new BehaviorProfileId(consumerId), consumerId, accounts, first, last,
                    total, succeeded, rejected, byType, volume, counterparties, frozenConsumables, categories);
        }
    }

    private static Map<ConsumableCategory, CategoryBehaviorProfile> aggregateCategories(Collection<ConsumableBehaviorProfile> values) {
        Map<ConsumableCategory, List<ConsumableBehaviorProfile>> grouped = new EnumMap<>(ConsumableCategory.class);
        values.forEach(value -> grouped.computeIfAbsent(value.category(), ignored -> new ArrayList<>()).add(value));
        Map<ConsumableCategory, CategoryBehaviorProfile> result = new EnumMap<>(ConsumableCategory.class);
        grouped.forEach((category, profiles) -> {
            long purchases = profiles.stream().mapToLong(ConsumableBehaviorProfile::purchaseCount).sum();
            long units = profiles.stream().mapToLong(ConsumableBehaviorProfile::unitsPurchased).sum();
            EnumMap<Currency, Integer> spent = new EnumMap<>(Currency.class);
            profiles.forEach(profile -> profile.totalSpentByCurrency().forEach((currency, amount) -> spent.merge(currency, amount, Math::addExact)));
            result.put(category, new CategoryBehaviorProfile(category, purchases, units, profiles.size(), spent));
        });
        return result;
    }

    private static final class MutableConsumable {
        private final String id;
        private final String name;
        private final ConsumableCategory category;
        private long purchases;
        private long units;
        private Instant first;
        private Instant last;
        private final EnumMap<Currency, Integer> spent = new EnumMap<>(Currency.class);
        private final EnumMap<Currency, Integer> min = new EnumMap<>(Currency.class);
        private final EnumMap<Currency, Integer> max = new EnumMap<>(Currency.class);

        private MutableConsumable(String id, String name, ConsumableCategory category) {
            this.id = id; this.name = name; this.category = category;
        }

        private void accept(Instant at, int quantity, int unitPrice, EconomicAmount amount) {
            purchases++; units += quantity;
            first = first == null || at.isBefore(first) ? at : first;
            last = last == null || at.isAfter(last) ? at : last;
            spent.merge(amount.currency(), amount.amount(), Math::addExact);
            min.merge(amount.currency(), unitPrice, Math::min);
            max.merge(amount.currency(), unitPrice, Math::max);
        }

        private ConsumableBehaviorProfile freeze() {
            return new ConsumableBehaviorProfile(id, name, category, purchases, units, spent, min, max, first, last);
        }
    }

    private static String required(Map<String, String> attributes, String key, EconomicEvent event) {
        String value = attributes.get(key);
        if (value == null || value.isBlank()) throw invalid(event, "missing purchase attribute: " + key);
        return value;
    }

    private static int positiveInt(String value, String field, EconomicEvent event) {
        int parsed = nonNegativeInt(value, field, event);
        if (parsed == 0) throw invalid(event, field + " must be positive");
        return parsed;
    }

    private static int nonNegativeInt(String value, String field, EconomicEvent event) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) throw invalid(event, field + " must not be negative");
            return parsed;
        } catch (NumberFormatException ex) { throw invalid(event, field + " is not an integer: " + value); }
    }

    private static BehaviorAggregationException invalid(EconomicEvent event, String detail) {
        return new BehaviorAggregationException("Cannot aggregate event " + event.id() + ": " + detail);
    }
}
