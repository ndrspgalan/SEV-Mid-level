package behavior.deviation.analysis;

import banking.identity.*;
import behavior.deviation.profile.*;
import behavior.expected.profile.*;
import behavior.temporal.*;
import behavior.temporal.profile.*;
import coinProperties.Currency;
import economicEvent.*;
import institutional.snapshot.*;

import java.util.*;

/**
 * Compares each complete population member with the empirical M3.1 reference.
 * It records signed distances, ranks and standardized positions, but never
 * translates them into anomaly, risk, suspicion or fraud.
 */
public final class BehaviorDeviationAnalyzer {
    private final SeasonResolver seasons;
    public BehaviorDeviationAnalyzer(SeasonResolver seasons){this.seasons=Objects.requireNonNull(seasons);}

    public BehaviorDeviationAnalysisReport analyze(Collection<SeasonSnapshot> snapshots,
                                                    Collection<ProfessionalBehaviorProfile> profiles,
                                                    Collection<ExpectedBehaviorSet> expectedSets,
                                                    Collection<EconomicEvent> events) {
        Objects.requireNonNull(snapshots); Objects.requireNonNull(profiles); Objects.requireNonNull(expectedSets); Objects.requireNonNull(events);
        Map<ProfileKey,ProfessionalBehaviorProfile> indexedProfiles=new HashMap<>(); long inconsistent=0;
        for(ProfessionalBehaviorProfile p:profiles){
            ProfileKey key=new ProfileKey(p.consumerId(),p.profession().code(),p.seasonPeriod().label());
            if(indexedProfiles.put(key,p)!=null) inconsistent++;
        }
        Map<ExpectedBehaviorSetId,ExpectedBehaviorSet> expectedById=new HashMap<>();
        for(ExpectedBehaviorSet set:expectedSets) if(expectedById.put(set.id(),set)!=null) inconsistent++;
        Map<ExpectedBehaviorSetId,PopulationSnapshot> populations=new HashMap<>();
        for(SeasonSnapshot snapshot:snapshots) for(PopulationSnapshot population:snapshot.populations().values())
            populations.put(ExpectedBehaviorSetId.of(population.profession().code(),population.seasonPeriod()),population);
        Map<SeasonConsumer,AuxiliaryActivity> auxiliary=auxiliary(events);

        List<BehaviorDeviationProfile> output=new ArrayList<>(); long sets=0,members=0,resolved=0,inactive=0,deviations=0;
        List<ExpectedBehaviorSet> ordered=expectedSets.stream().sorted(Comparator.comparing((ExpectedBehaviorSet s)->s.seasonPeriod().startsOn()).thenComparing(s->s.profession().code().value())).toList();
        for(ExpectedBehaviorSet expected:ordered){
            sets++;
            PopulationSnapshot population=populations.get(expected.id());
            if(population==null){inconsistent++;continue;}
            List<MemberEvidence> evidence=new ArrayList<>();
            for(ConsumerId consumer:population.registeredConsumers()){
                members++;
                ProfessionalBehaviorProfile profile=indexedProfiles.get(new ProfileKey(consumer,expected.profession().code(),expected.seasonPeriod().label()));
                if(profile==null) inactive++; else resolved++;
                evidence.add(new MemberEvidence(consumer,profile,auxiliary.getOrDefault(new SeasonConsumer(consumer,expected.seasonPeriod().label()),AuxiliaryActivity.EMPTY)));
            }
            Map<ConsumerId,Map<ExpectedBehaviorMetric,Double>> values=new LinkedHashMap<>();
            for(MemberEvidence member:evidence) values.put(member.consumerId(),values(member));
            for(MemberEvidence member:evidence){
                TreeMap<ExpectedBehaviorMetric,BehaviorDeviation> memberDeviations=new TreeMap<>();
                for(var entry:expected.metrics().entrySet()){
                    double observed=values.get(member.consumerId()).getOrDefault(entry.getKey(),0d);
                    List<Double> distribution=new ArrayList<>();
                    for(Map<ExpectedBehaviorMetric,Double> each:values.values()) distribution.add(each.getOrDefault(entry.getKey(),0d));
                    memberDeviations.put(entry.getKey(),BehaviorDeviation.compare(observed,entry.getValue(),OptionalDouble.of(percentileRank(observed,distribution))));
                }
                deviations+=memberDeviations.size();
                output.add(new BehaviorDeviationProfile(BehaviorDeviationProfileId.of(member.consumerId(),expected.profession().code(),expected.seasonPeriod()),
                        member.consumerId(),expected.profession(),expected.seasonPeriod(),expected.id(),memberDeviations));
            }
        }
        return new BehaviorDeviationAnalysisReport(sets,members,resolved,inactive,inconsistent,deviations,output);
    }

    /** Midrank percentile: values below plus half of tied values, divided by N. */
    private static double percentileRank(double value,List<Double> distribution){
        if(distribution.isEmpty()) throw new IllegalArgumentException("empty percentile population");
        long below=distribution.stream().filter(v->v<value).count();
        long equal=distribution.stream().filter(v->Double.compare(v,value)==0).count();
        return 100d*(below+0.5d*equal)/distribution.size();
    }

    private Map<ExpectedBehaviorMetric,Double> values(MemberEvidence member){
        TreeMap<ExpectedBehaviorMetric,Double> result=new TreeMap<>(); ProfessionalBehaviorProfile profile=member.profile();
        if(profile!=null){
            put(result,ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.SEASON_ACTIVITY_TOTAL,"ALL"),profile.seasonActivity().total());
            profile.seasonActivity().byDayPeriod().forEach((period,value)->put(result,ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.DAY_PERIOD_ACTIVITY,period.name()),value));
            profile.eventTypeSeasonActivity().forEach((type,summary)->put(result,ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.EVENT_COUNT,type.name()),summary.total()));
            profile.eventTypes().forEach((type,temporal)->temporal.byWindow().forEach((window,s)->put(result,ExpectedBehaviorMetric.windowed(ExpectedBehaviorMetricFamily.EVENT_FREQUENCY,type.name(),window),s.mean())));
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
        put(result,ExpectedBehaviorMetric.of(count,subject),seasonTotal(purchase.purchases())); put(result,ExpectedBehaviorMetric.of(units,subject),seasonTotal(purchase.units()));
        purchase.purchases().byWindow().forEach((window,s)->put(result,ExpectedBehaviorMetric.windowed(countFrequency,subject,window),s.mean()));
        purchase.units().byWindow().forEach((window,s)->put(result,ExpectedBehaviorMetric.windowed(unitsFrequency,subject,window),s.mean()));
        purchase.monetaryVolume().forEach((currency,temporal)->{put(result,ExpectedBehaviorMetric.monetary(money,subject,currency),seasonTotal(temporal)); temporal.byWindow().forEach((window,s)->put(result,ExpectedBehaviorMetric.monetaryWindowed(moneyFrequency,subject,currency,window),s.mean()));});
    }
    private static double seasonTotal(TemporalBehaviorStatistics statistics){return statistics.byWindow().values().iterator().next().totalOccurrences();}
    private static void put(Map<ExpectedBehaviorMetric,Double> target,ExpectedBehaviorMetric key,double value){target.put(key,value);}

    private Map<SeasonConsumer,AuxiliaryActivity> auxiliary(Collection<EconomicEvent> events){
        Map<SeasonConsumer,MutableAuxiliary> result=new HashMap<>();
        for(EconomicEvent event:events){
            SeasonPeriod period=seasons.resolve(event.occurredAt()); SeasonConsumer actor=new SeasonConsumer(event.actor().consumerId(),period.label());
            MutableAuxiliary actorValue=result.computeIfAbsent(actor,k->new MutableAuxiliary()); event.counterparty().ifPresent(c->actorValue.counterparties.add(c.accountId()));
            if(!event.rejected()&&event.type()==EconomicEventType.FUNDS_TRANSFERRED){actorValue.sent++; event.counterparty().flatMap(EconomicCounterparty::consumerId).ifPresent(recipient->{MutableAuxiliary rv=result.computeIfAbsent(new SeasonConsumer(recipient,period.label()),k->new MutableAuxiliary());rv.received++;rv.counterparties.add(event.actor().accountId());});}
        }
        Map<SeasonConsumer,AuxiliaryActivity> frozen=new HashMap<>(); result.forEach((k,v)->frozen.put(k,new AuxiliaryActivity(v.counterparties,v.sent,v.received))); return frozen;
    }
    private record ProfileKey(ConsumerId consumerId,ProfessionCode professionCode,String season){}
    private record SeasonConsumer(ConsumerId consumerId,String season){}
    private record MemberEvidence(ConsumerId consumerId,ProfessionalBehaviorProfile profile,AuxiliaryActivity auxiliary){}
    private static final class MutableAuxiliary{final Set<BankAccountId> counterparties=new LinkedHashSet<>();long sent,received;}
    private record AuxiliaryActivity(Set<BankAccountId> counterparties,long transfersSent,long transfersReceived){
        static final AuxiliaryActivity EMPTY=new AuxiliaryActivity(Set.of(),0,0);
        AuxiliaryActivity{counterparties=Collections.unmodifiableSet(new LinkedHashSet<>(counterparties));}
    }
}
