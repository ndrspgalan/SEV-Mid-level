package application.deviation;

import behavior.deviation.analysis.*;
import behavior.deviation.profile.*;
import behavior.deviation.repository.*;
import behavior.expected.repository.ExpectedBehaviorSetRepository;
import behavior.temporal.repository.ProfessionalBehaviorProfileRepository;
import economicEvent.repository.EconomicEventRepository;
import institutional.repository.SeasonSnapshotRepository;
import java.util.*;

/** Rebuilds M3.2 descriptive comparisons from persisted M2 and M3.1 projections. */
public final class BehaviorDeviationProfileService {
    private final SeasonSnapshotRepository populations; private final ProfessionalBehaviorProfileRepository profiles;
    private final ExpectedBehaviorSetRepository expected; private final EconomicEventRepository events;
    private final BehaviorDeviationProfileRepository deviations; private final BehaviorDeviationAnalyzer analyzer;
    private BehaviorDeviationAnalysisReport lastReport=new BehaviorDeviationAnalysisReport(0,0,0,0,0,0,List.of());
    public BehaviorDeviationProfileService(SeasonSnapshotRepository populations,ProfessionalBehaviorProfileRepository profiles,ExpectedBehaviorSetRepository expected,EconomicEventRepository events,BehaviorDeviationProfileRepository deviations,BehaviorDeviationAnalyzer analyzer){
        this.populations=Objects.requireNonNull(populations);this.profiles=Objects.requireNonNull(profiles);this.expected=Objects.requireNonNull(expected);this.events=Objects.requireNonNull(events);this.deviations=Objects.requireNonNull(deviations);this.analyzer=Objects.requireNonNull(analyzer);
    }
    public BehaviorDeviationAnalysisReport rebuild(){lastReport=analyzer.analyze(populations.findAll(),profiles.findAll(),expected.findAll(),events.findAll());deviations.replaceAll(lastReport.deviationProfiles());return lastReport;}
    public BehaviorDeviationAnalysisReport lastReport(){return lastReport;}
    public Optional<BehaviorDeviationProfile> findById(BehaviorDeviationProfileId id){return deviations.findById(id);}
    public List<BehaviorDeviationProfile> findAll(){return deviations.findAll();}
    public long count(){return deviations.count();}
}
