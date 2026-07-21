package application.analytics;

import banking.identity.BankAccountId;
import banking.identity.ConsumerId;
import coinProperties.Currency;
import economicEvent.*;
import economicEvent.query.EconomicEventQuery;
import economicEvent.query.EconomicEventStatistics;

import java.util.*;

/** Computes aggregate descriptions over the complete filtered result, independent of pagination. */
public final class EconomicEventStatisticsService {
    private final EconomicEventQueryService queryService;

    public EconomicEventStatisticsService(EconomicEventQueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService, "queryService must not be null");
    }

    public EconomicEventStatistics calculate(EconomicEventQuery query) {
        List<EconomicEvent> events = queryService.matchingEvents(Objects.requireNonNull(query, "query must not be null"));
        Map<EconomicEventType, Long> byType = zeroed(EconomicEventType.values());
        Map<EconomicEventCategory, Long> byCategory = zeroed(EconomicEventCategory.values());
        Map<EconomicEventStatus, Long> byStatus = zeroed(EconomicEventStatus.values());
        Map<EconomicEventSourceType, Long> bySource = zeroed(EconomicEventSourceType.values());
        Map<Currency, Long> volume = zeroed(Currency.values());
        Set<BankAccountId> accounts = new HashSet<>();
        Set<ConsumerId> consumers = new HashSet<>();
        long monetaryEvents = 0;
        long rejectedEvents = 0;

        for (EconomicEvent event : events) {
            increment(byType, event.type());
            increment(byCategory, event.category());
            increment(byStatus, event.status());
            increment(bySource, event.source().type());
            accounts.add(event.actor().accountId());
            consumers.add(event.actor().consumerId());
            if (event.monetary()) monetaryEvents++;
            if (event.rejected()) rejectedEvents++;
            event.primaryAmount().ifPresent(amount -> add(volume, amount));
            event.secondaryAmount().ifPresent(amount -> add(volume, amount));
        }

        return new EconomicEventStatistics(events.size(), byType, byCategory, byStatus, bySource,
                volume, monetaryEvents, rejectedEvents, accounts.size(), consumers.size());
    }

    private static <E extends Enum<E>> Map<E, Long> zeroed(E[] values) {
        EnumMap<E, Long> map = new EnumMap<>(values[0].getDeclaringClass());
        for (E value : values) map.put(value, 0L);
        return map;
    }
    private static <K> void increment(Map<K, Long> map, K key) { map.compute(key, (ignored, count) -> count + 1); }
    private static void add(Map<Currency, Long> map, EconomicAmount amount) {
        map.compute(amount.currency(), (ignored, total) -> Math.addExact(total, (long) amount.amount()));
    }
}
