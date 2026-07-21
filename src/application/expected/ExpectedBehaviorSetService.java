package application.expected;

import behavior.expected.analysis.*;
import behavior.expected.profile.*;
import behavior.expected.repository.*;
import behavior.temporal.repository.ProfessionalBehaviorProfileRepository;
import economicEvent.repository.EconomicEventRepository;
import institutional.repository.SeasonSnapshotRepository;
import java.util.*;

/** Rebuilds M3.1 collective references from persisted M2 projections. */
public final class ExpectedBehaviorSetService {
    private final SeasonSnapshotRepository populations;
    private final ProfessionalBehaviorProfileRepository profiles;
    private final EconomicEventRepository events;
    private final ExpectedBehaviorSetRepository expected;
    private final ExpectedBehaviorAnalyzer analyzer;
    private ExpectedBehaviorAnalysisReport lastReport=new ExpectedBehaviorAnalysisReport(0,0,0,0,0,0,List.of());
    public ExpectedBehaviorSetService(SeasonSnapshotRepository populations,ProfessionalBehaviorProfileRepository profiles,EconomicEventRepository events,ExpectedBehaviorSetRepository expected,ExpectedBehaviorAnalyzer analyzer){
        this.populations=Objects.requireNonNull(populations);this.profiles=Objects.requireNonNull(profiles);this.events=Objects.requireNonNull(events);this.expected=Objects.requireNonNull(expected);this.analyzer=Objects.requireNonNull(analyzer);
    }
    public ExpectedBehaviorAnalysisReport rebuild(){
        lastReport=analyzer.analyze(populations.findAll(),profiles.findAll(),events.findAll());
        expected.replaceAll(lastReport.expectedBehaviorSets());return lastReport;
    }
    public ExpectedBehaviorAnalysisReport lastReport(){return lastReport;}
    public Optional<ExpectedBehaviorSet> findById(ExpectedBehaviorSetId id){return expected.findById(id);}
    public List<ExpectedBehaviorSet> findAll(){return expected.findAll();}
    public long count(){return expected.count();}
}
