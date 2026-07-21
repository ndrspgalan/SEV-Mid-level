package behavior.temporal.analysis;

import banking.identity.ConsumerId;
import banking.identity.Profession;
import behavior.temporal.*;
import behavior.temporal.profile.*;
import coinProperties.Currency;
import consumableRegistry.ConsumableCategory;
import economicEvent.*;
import operationalControl.profile.*;

import java.time.*;
import java.util.*;

/**
 * Builds profession- and season-safe individual profiles from frozen M1 facts.
 *
 * <p>The temporal bound always governs cohort attribution. Current census data
 * must never rewrite the profession preserved by the historical event. The
 * analyzer only organizes evidence: collective expectation, risk direction,
 * correlation, Inspection and Fraud belong to later layers.</p>
 */
public final class ProfessionalBehaviorAnalyzer {
    private final ProfessionCreditProfileResolver creditProfiles;
    private final SeasonResolver seasons;
    private final TemporalStatisticsCalculator statistics;
    private final ZoneId zone;

    public ProfessionalBehaviorAnalyzer(ProfessionCreditProfileResolver creditProfiles, SeasonResolver seasons, ZoneId zone) {
        this.creditProfiles = Objects.requireNonNull(creditProfiles);
        this.seasons = Objects.requireNonNull(seasons);
        this.zone = Objects.requireNonNull(zone);
        this.statistics = new TemporalStatisticsCalculator(zone);
    }

    public List<ProfessionalBehaviorProfile> analyze(Iterable<EconomicEvent> events) {
        return analyzeWithReport(events).profiles();
    }

    public ProfessionalBehaviorAnalysisReport analyzeWithReport(Iterable<EconomicEvent> events) {
        Map<Key, Mutable> grouped = new TreeMap<>(Comparator.comparing(Key::sortKey));
        long examined = 0;
        long omitted = 0;
        for (EconomicEvent event : events) {
            examined++;
            Objects.requireNonNull(event);
            if (event.actorProfession().isEmpty()) {
                omitted++;
                continue;
            }
            Profession profession = event.actorProfession().orElseThrow();
            SeasonPeriod season = seasons.resolve(event.occurredAt());
            Key key = new Key(event.actor().consumerId(), profession, season);
            grouped.computeIfAbsent(key, ignored -> new Mutable(key)).accept(event);
        }
        List<ProfessionalBehaviorProfile> result = new ArrayList<>();
        for (Mutable value : grouped.values()) result.add(value.freeze());
        return new ProfessionalBehaviorAnalysisReport(examined, omitted, result);
    }

    private record Key(ConsumerId consumerId, Profession profession, SeasonPeriod season) {
        String sortKey() { return consumerId + "|" + profession.code() + "|" + season.label(); }
    }

    private final class Mutable {
        final Key key;
        Instant first,last;
        long count;
        final List<Instant> all = new ArrayList<>();
        final Map<EconomicEventType,List<Instant>> types = new EnumMap<>(EconomicEventType.class);
        final Map<String,List<PurchaseBehaviorObservation>> consumables = new LinkedHashMap<>();
        final Map<ConsumableCategory,List<PurchaseBehaviorObservation>> categories = new EnumMap<>(ConsumableCategory.class);

        Mutable(Key key) { this.key=key; }

        void accept(EconomicEvent event) {
            count++;
            all.add(event.occurredAt());
            first=first==null||event.occurredAt().isBefore(first)?event.occurredAt():first;
            last=last==null||event.occurredAt().isAfter(last)?event.occurredAt():last;
            types.computeIfAbsent(event.type(), ignored -> new ArrayList<>()).add(event.occurredAt());

            if (!event.rejected() && event.type()==EconomicEventType.PURCHASE_EXECUTED) {
                String id = requiredAttribute(event, "consumableId");
                ConsumableCategory category = ConsumableCategory.valueOf(requiredAttribute(event, "consumableCategory"));
                int quantity = parsePositive(event, "quantity");
                EconomicAmount amount = event.primaryAmount().orElseThrow(() -> new IllegalArgumentException("purchase event lacks amount: " + event.id()));
                PurchaseBehaviorObservation observation = new PurchaseBehaviorObservation(
                        event.occurredAt(), quantity, amount.currency(), amount.amount());
                consumables.computeIfAbsent(id, ignored -> new ArrayList<>()).add(observation);
                categories.computeIfAbsent(category, ignored -> new ArrayList<>()).add(observation);
            }
        }

        ProfessionalBehaviorProfile freeze() {
            return new ProfessionalBehaviorProfile(
                    ProfessionalBehaviorProfileId.of(key.consumerId,key.profession.code(),key.season),
                    key.consumerId,key.profession,creditProfiles.resolve(key.profession),key.season,
                    first,last,count,seasonSummary(all),convertTypes(),typeSummaries(),convertPurchases(consumables),convertPurchases(categories));
        }

        private Map<EconomicEventType, TemporalBehaviorStatistics> convertTypes() {
            LinkedHashMap<EconomicEventType,TemporalBehaviorStatistics> result = new LinkedHashMap<>();
            types.forEach((type, values) -> result.put(type, statistics.calculate(values,key.season)));
            return result;
        }

        private Map<EconomicEventType, SeasonActivitySummary> typeSummaries() {
            LinkedHashMap<EconomicEventType,SeasonActivitySummary> result = new LinkedHashMap<>();
            types.forEach((type, values) -> result.put(type, seasonSummary(values)));
            return result;
        }

        private <K> Map<K, PurchaseBehaviorStatistics> convertPurchases(Map<K,List<PurchaseBehaviorObservation>> source) {
            LinkedHashMap<K,PurchaseBehaviorStatistics> result = new LinkedHashMap<>();
            source.forEach((key, values) -> {
                List<Instant> occurrences = values.stream().map(PurchaseBehaviorObservation::occurredAt).toList();
                List<TimedValue> units = values.stream().map(value -> new TimedValue(value.occurredAt(), value.quantity())).toList();
                EnumMap<Currency,TemporalBehaviorStatistics> monetary = new EnumMap<>(Currency.class);
                for (Currency currency : Currency.values()) {
                    List<TimedValue> amounts = values.stream()
                            .filter(value -> value.currency() == currency)
                            .map(value -> new TimedValue(value.occurredAt(), value.totalPrice()))
                            .toList();
                    if (!amounts.isEmpty()) monetary.put(currency, statistics.calculateWeighted(amounts, key().season));
                }
                result.put(key, new PurchaseBehaviorStatistics(
                        statistics.calculate(occurrences, key().season),
                        statistics.calculateWeighted(units, key().season), monetary));
            });
            return result;
        }

        private Key key() { return key; }

        private SeasonActivitySummary seasonSummary(List<Instant> values) {
            EnumMap<DayPeriod,Long> periods = new EnumMap<>(DayPeriod.class);
            for (Instant value : values) periods.merge(DayPeriod.from(value.atZone(zone).toLocalTime()),1L,Long::sum);
            Instant earliest = values.stream().min(Instant::compareTo).orElseThrow();
            Instant latest = values.stream().max(Instant::compareTo).orElseThrow();
            return new SeasonActivitySummary(values.size(), earliest, latest, periods);
        }

        private String requiredAttribute(EconomicEvent event, String name) {
            String value = event.attributes().get(name);
            if (value == null || value.isBlank()) throw new IllegalArgumentException("purchase event lacks " + name + ": " + event.id());
            return value;
        }

        private int parsePositive(EconomicEvent event, String name) {
            try {
                int value = Integer.parseInt(requiredAttribute(event, name));
                if (value <= 0) throw new NumberFormatException();
                return value;
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("purchase event has invalid " + name + ": " + event.id());
            }
        }
    }
}
