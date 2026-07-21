package behavior.expected.analysis;

import banking.identity.*;
import behavior.expected.profile.*;
import behavior.temporal.*;
import behavior.temporal.profile.*;
import coinProperties.Currency;
import consumableRegistry.ConsumableCategory;
import economicEvent.*;
import institutional.snapshot.*;

import java.time.*;
import java.util.*;

/**
 * Joins M2.4 population boundaries with M2.3+ individual evidence and builds
 * collective empirical distributions. It does not compare, score or classify
 * any holder. Confirmed population members without a metric contribute zero.
 */
public final class ExpectedBehaviorAnalyzer {
    private final PopulationStatisticsCalculator statistics=new PopulationStatisticsCalculator();
    private final SeasonResolver seasons;

    public ExpectedBehaviorAnalyzer(SeasonResolver seasons){this.seasons=Objects.requireNonNull(seasons);}

    public ExpectedBehaviorAnalysisReport analyze(Collection<SeasonSnapshot> seasonsSnapshots,
                                                   Collection<ProfessionalBehaviorProfile> profiles,
                                                   Collection<EconomicEvent> events){
        Objects.requireNonNull(seasonsSnapshots);Objects.requireNonNull(profiles);Objects.requireNonNull(events);
        Map<ProfileKey,ProfessionalBehaviorProfile> indexed=new HashMap<>(); long inconsistent=0;
        for(ProfessionalBehaviorProfile profile:profiles){
            ProfileKey key=new ProfileKey(profile.consumerId(),profile.profession().code(),profile.seasonPeriod().label());
            if(indexed.put(key,profile)!=null)inconsistent++;
        }
        Map<SeasonConsumer,AuxiliaryActivity> auxiliary=auxiliary(events);
        List<ExpectedBehaviorSet> sets=new ArrayList<>(); long populations=0,members=0,resolved=0,inactive=0,metricCount=0;
        List<SeasonSnapshot> ordered=seasonsSnapshots.stream().sorted(Comparator.comparing(s->s.seasonPeriod().startsOn())).toList();
        for(SeasonSnapshot seasonSnapshot:ordered){
            for(PopulationSnapshot population:seasonSnapshot.populations().values()){
                populations++; members+=population.populationSize();
                List<MemberEvidence> evidence=new ArrayList<>();
                for(ConsumerId consumer:population.registeredConsumers()){
                    ProfessionalBehaviorProfile profile=indexed.get(new ProfileKey(consumer,population.profession().code(),population.seasonPeriod().label()));
                    if(profile==null)inactive++; else resolved++;
                    evidence.add(new MemberEvidence(consumer,profile,auxiliary.getOrDefault(new SeasonConsumer(consumer,population.seasonPeriod().label()),AuxiliaryActivity.EMPTY)));
                }
                Map<ExpectedBehaviorMetric,PopulationStatistics> metrics=population.populationSize()==0?Map.of():buildMetrics(evidence);
                metricCount+=metrics.size();
                sets.add(new ExpectedBehaviorSet(ExpectedBehaviorSetId.of(population.profession().code(),population.seasonPeriod()),
                        population.profession(),population.seasonPeriod(),population.populationSize(),metrics));
            }
        }
        return new ExpectedBehaviorAnalysisReport(populations,members,resolved,inactive,inconsistent,metricCount,sets);
    }

    private Map<ExpectedBehaviorMetric,PopulationStatistics> buildMetrics(List<MemberEvidence> members){
        TreeSet<ExpectedBehaviorMetric> universe=new TreeSet<>();
        for(MemberEvidence member:members)universe.addAll(values(member).keySet());
        // Population-wide operational metrics exist even when every value is zero.
        universe.add(ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.COUNTERPARTY_COUNT,"ALL"));
        universe.add(ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.TRANSFERS_SENT,"FUNDS_TRANSFERRED"));
        universe.add(ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.TRANSFERS_RECEIVED,"FUNDS_TRANSFERRED"));
        TreeMap<ExpectedBehaviorMetric,PopulationStatistics> result=new TreeMap<>();
        Map<ConsumerId,Map<ExpectedBehaviorMetric,Double>> all=new LinkedHashMap<>();
        for(MemberEvidence member:members)all.put(member.consumerId(),values(member));
        for(ExpectedBehaviorMetric metric:universe){
            List<Double> observations=new ArrayList<>();
            for(Map<ExpectedBehaviorMetric,Double> member:all.values())observations.add(member.getOrDefault(metric,0d));
            result.put(metric,statistics.calculate(observations));
        }
        return result;
    }

    private Map<ExpectedBehaviorMetric,Double> values(MemberEvidence member){
        TreeMap<ExpectedBehaviorMetric,Double> result=new TreeMap<>();
        ProfessionalBehaviorProfile profile=member.profile();
        if(profile!=null){
            put(result,ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.SEASON_ACTIVITY_TOTAL,"ALL"),profile.seasonActivity().total());
            profile.seasonActivity().byDayPeriod().forEach((period,value)->put(result,ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.DAY_PERIOD_ACTIVITY,period.name()),value));
            profile.eventTypeSeasonActivity().forEach((type,summary)->put(result,ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.EVENT_COUNT,type.name()),summary.total()));
            profile.eventTypes().forEach((type,temporal)->temporal.byWindow().forEach((window,windowStats)->
                    put(result,ExpectedBehaviorMetric.windowed(ExpectedBehaviorMetricFamily.EVENT_FREQUENCY,type.name(),window),windowStats.mean())));
            profile.consumables().forEach((id,purchase)->purchaseValues(result,id,purchase,true));
            profile.categories().forEach((category,purchase)->purchaseValues(result,category.name(),purchase,false));
        }
        put(result,ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.COUNTERPARTY_COUNT,"ALL"),member.auxiliary().counterparties().size());
        put(result,ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.TRANSFERS_SENT,"FUNDS_TRANSFERRED"),member.auxiliary().transfersSent());
        put(result,ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.TRANSFERS_RECEIVED,"FUNDS_TRANSFERRED"),member.auxiliary().transfersReceived());
        return result;
    }

    private void purchaseValues(Map<ExpectedBehaviorMetric,Double> result,String subject,PurchaseBehaviorStatistics purchase,boolean consumable){
        ExpectedBehaviorMetricFamily count=consumable?ExpectedBehaviorMetricFamily.CONSUMABLE_PURCHASE_COUNT:ExpectedBehaviorMetricFamily.CATEGORY_PURCHASE_COUNT;
        ExpectedBehaviorMetricFamily countFrequency=consumable?ExpectedBehaviorMetricFamily.CONSUMABLE_PURCHASE_FREQUENCY:ExpectedBehaviorMetricFamily.CATEGORY_PURCHASE_FREQUENCY;
        ExpectedBehaviorMetricFamily units=consumable?ExpectedBehaviorMetricFamily.CONSUMABLE_UNIT_COUNT:ExpectedBehaviorMetricFamily.CATEGORY_UNIT_COUNT;
        ExpectedBehaviorMetricFamily unitsFrequency=consumable?ExpectedBehaviorMetricFamily.CONSUMABLE_UNIT_FREQUENCY:ExpectedBehaviorMetricFamily.CATEGORY_UNIT_FREQUENCY;
        ExpectedBehaviorMetricFamily money=consumable?ExpectedBehaviorMetricFamily.CONSUMABLE_MONETARY_VOLUME:ExpectedBehaviorMetricFamily.CATEGORY_MONETARY_VOLUME;
        ExpectedBehaviorMetricFamily moneyFrequency=consumable?ExpectedBehaviorMetricFamily.CONSUMABLE_MONETARY_VOLUME_FREQUENCY:ExpectedBehaviorMetricFamily.CATEGORY_MONETARY_VOLUME_FREQUENCY;
        put(result,ExpectedBehaviorMetric.of(count,subject),seasonTotal(purchase.purchases()));
        put(result,ExpectedBehaviorMetric.of(units,subject),seasonTotal(purchase.units()));
        purchase.purchases().byWindow().forEach((window,s)->put(result,ExpectedBehaviorMetric.windowed(countFrequency,subject,window),s.mean()));
        purchase.units().byWindow().forEach((window,s)->put(result,ExpectedBehaviorMetric.windowed(unitsFrequency,subject,window),s.mean()));
        purchase.monetaryVolume().forEach((currency,temporal)->{
            put(result,ExpectedBehaviorMetric.monetary(money,subject,currency),seasonTotal(temporal));
            temporal.byWindow().forEach((window,s)->put(result,ExpectedBehaviorMetric.monetaryWindowed(moneyFrequency,subject,currency,window),s.mean()));
        });
    }
    private static double seasonTotal(TemporalBehaviorStatistics statistics){return statistics.byWindow().values().iterator().next().totalOccurrences();}
    private static void put(Map<ExpectedBehaviorMetric,Double> target,ExpectedBehaviorMetric key,double value){target.put(key,value);}

    private Map<SeasonConsumer,AuxiliaryActivity> auxiliary(Collection<EconomicEvent> events){
        Map<SeasonConsumer,MutableAuxiliary> result=new HashMap<>();
        for(EconomicEvent event:events){
            SeasonPeriod period=seasons.resolve(event.occurredAt());
            SeasonConsumer actor=new SeasonConsumer(event.actor().consumerId(),period.label());
            MutableAuxiliary actorValue=result.computeIfAbsent(actor,k->new MutableAuxiliary());
            event.counterparty().ifPresent(c->actorValue.counterparties.add(c.accountId()));
            if(!event.rejected()&&event.type()==EconomicEventType.FUNDS_TRANSFERRED){
                actorValue.sent++;
                event.counterparty().flatMap(EconomicCounterparty::consumerId).ifPresent(recipient->{
                    MutableAuxiliary recipientValue=result.computeIfAbsent(new SeasonConsumer(recipient,period.label()),k->new MutableAuxiliary());
                    recipientValue.received++;
                    recipientValue.counterparties.add(event.actor().accountId());
                });
            }
        }
        Map<SeasonConsumer,AuxiliaryActivity> frozen=new HashMap<>();
        result.forEach((k,v)->frozen.put(k,new AuxiliaryActivity(v.counterparties,v.sent,v.received)));
        return frozen;
    }

    private record ProfileKey(ConsumerId consumerId,ProfessionCode professionCode,String season){}
    private record SeasonConsumer(ConsumerId consumerId,String season){}
    private record MemberEvidence(ConsumerId consumerId,ProfessionalBehaviorProfile profile,AuxiliaryActivity auxiliary){}
    private static final class MutableAuxiliary{final Set<BankAccountId> counterparties=new LinkedHashSet<>();long sent,received;}
    private record AuxiliaryActivity(Set<BankAccountId> counterparties,long transfersSent,long transfersReceived){
        static final AuxiliaryActivity EMPTY=new AuxiliaryActivity(Set.of(),0,0);
        AuxiliaryActivity{counterparties=Collections.unmodifiableSet(new LinkedHashSet<>(counterparties));if(transfersSent<0||transfersReceived<0)throw new IllegalArgumentException();}
    }
}
