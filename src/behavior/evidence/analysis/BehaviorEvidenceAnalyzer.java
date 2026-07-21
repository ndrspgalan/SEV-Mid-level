package behavior.evidence.analysis;

import behavior.deviation.profile.*;
import behavior.evidence.casefile.*;
import behavior.expected.profile.*;
import institutional.snapshot.*;
import java.util.*;

/** Organizes M3.2 deviations into reproducible case files without filtering or scoring them. */
public final class BehaviorEvidenceAnalyzer {
    public BehaviorEvidenceAnalysisReport analyze(Collection<BehaviorDeviationProfile> profiles,Collection<SeasonSnapshot> snapshots){
        Objects.requireNonNull(profiles);Objects.requireNonNull(snapshots);
        Map<PopulationKey,PopulationSnapshot> populations=new HashMap<>();long inconsistent=0;
        for(SeasonSnapshot season:snapshots) for(PopulationSnapshot population:season.populations().values())
            if(populations.put(new PopulationKey(population.profession().code().value(),population.seasonPeriod().label()),population)!=null) inconsistent++;
        List<SeasonSnapshot> orderedSeasons=snapshots.stream().sorted(Comparator.comparing(s->s.seasonPeriod().startsOn())).toList();
        Map<String,SeasonSnapshot> previousBySeason=new HashMap<>();
        for(int i=1;i<orderedSeasons.size();i++) previousBySeason.put(orderedSeasons.get(i).seasonPeriod().label(),orderedSeasons.get(i-1));
        List<BehaviorEvidenceSet> output=new ArrayList<>();long examined=0,entries=0,missing=0;
        List<BehaviorDeviationProfile> ordered=profiles.stream().sorted(Comparator.comparing((BehaviorDeviationProfile p)->p.seasonPeriod().startsOn()).thenComparing(p->p.profession().code().value()).thenComparing(p->p.consumerId().value())).toList();
        for(BehaviorDeviationProfile profile:ordered){
            examined++;
            PopulationSnapshot population=populations.get(new PopulationKey(profile.profession().code().value(),profile.seasonPeriod().label()));
            if(population==null){missing++;continue;}
            PopulationSnapshot previous=null;SeasonSnapshot previousSeason=previousBySeason.get(profile.seasonPeriod().label());
            if(previousSeason!=null) previous=previousSeason.populations().get(profile.profession().code());
            InstitutionalContext context=context(population,previous);
            EnumMap<BehaviorEvidenceCategory,List<BehaviorEvidence>> grouped=new EnumMap<>(BehaviorEvidenceCategory.class);
            for(var entry:profile.deviations().entrySet()){
                BehaviorEvidenceCategory category=category(entry.getKey().family());
                grouped.computeIfAbsent(category,k->new ArrayList<>()).add(BehaviorEvidence.of(entry.getKey(),category,entry.getValue()));entries++;
            }
            grouped.values().forEach(list->list.sort(Comparator.comparing(e->e.metric())));
            output.add(new BehaviorEvidenceSet(BehaviorEvidenceSetId.of(profile.consumerId(),profile.profession().code(),profile.seasonPeriod()),profile.consumerId(),profile.profession(),profile.seasonPeriod(),profile.id(),profile.expectedBehaviorSetId(),context,grouped));
        }
        return new BehaviorEvidenceAnalysisReport(examined,output.size(),entries,missing,inconsistent,output);
    }
    private static InstitutionalContext context(PopulationSnapshot p,PopulationSnapshot previous){
        return new InstitutionalContext(p.profession(),p.seasonPeriod(),p.populationSize(),p.registrations(),p.professionChangesIn(),p.professionChangesOut(),p.holderAssignments(),p.holderReleases(),p.deaths(),p.accountClosures(),p.transfersSent(),p.transfersReceived(),p.upwardMobilityIn(),p.upwardMobilityOut(),p.downwardMobilityIn(),p.downwardMobilityOut(),
                previous==null?OptionalLong.empty():OptionalLong.of(p.populationSize()-previous.populationSize()),
                previous==null?OptionalLong.empty():OptionalLong.of((p.transfersReceived()-p.transfersSent())-(previous.transfersReceived()-previous.transfersSent())),
                previous==null?OptionalLong.empty():OptionalLong.of(p.netUpwardMobility()-previous.netUpwardMobility()),
                previous==null?OptionalLong.empty():OptionalLong.of(p.netDownwardMobility()-previous.netDownwardMobility()));
    }
    private static BehaviorEvidenceCategory category(ExpectedBehaviorMetricFamily family){
        return switch(family){
            case SEASON_ACTIVITY_TOTAL,EVENT_COUNT,EVENT_FREQUENCY -> BehaviorEvidenceCategory.ECONOMIC_ACTIVITY;
            case DAY_PERIOD_ACTIVITY -> BehaviorEvidenceCategory.TEMPORAL_ACTIVITY;
            case CONSUMABLE_PURCHASE_COUNT,CONSUMABLE_PURCHASE_FREQUENCY,CONSUMABLE_UNIT_COUNT,CONSUMABLE_UNIT_FREQUENCY,
                    CATEGORY_PURCHASE_COUNT,CATEGORY_PURCHASE_FREQUENCY,CATEGORY_UNIT_COUNT,CATEGORY_UNIT_FREQUENCY -> BehaviorEvidenceCategory.CONSUMPTION;
            case CONSUMABLE_MONETARY_VOLUME,CONSUMABLE_MONETARY_VOLUME_FREQUENCY,CATEGORY_MONETARY_VOLUME,CATEGORY_MONETARY_VOLUME_FREQUENCY -> BehaviorEvidenceCategory.MONETARY_ACTIVITY;
            case COUNTERPARTY_COUNT,TRANSFERS_SENT,TRANSFERS_RECEIVED -> BehaviorEvidenceCategory.TRANSFER_NETWORK;
        };
    }
    private record PopulationKey(String profession,String season){}
}
