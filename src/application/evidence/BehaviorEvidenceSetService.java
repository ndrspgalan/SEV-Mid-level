package application.evidence;
import behavior.deviation.repository.BehaviorDeviationProfileRepository;import behavior.evidence.analysis.*;import behavior.evidence.casefile.*;import behavior.evidence.repository.*;import institutional.repository.SeasonSnapshotRepository;import java.util.*;
/** Rebuilds immutable M3.3 analytical case files from M3.2 and M2.4 projections. */
public final class BehaviorEvidenceSetService{
 private final BehaviorDeviationProfileRepository deviations;private final SeasonSnapshotRepository snapshots;private final BehaviorEvidenceSetRepository evidence;private final BehaviorEvidenceAnalyzer analyzer;
 private BehaviorEvidenceAnalysisReport lastReport=new BehaviorEvidenceAnalysisReport(0,0,0,0,0,List.of());
 public BehaviorEvidenceSetService(BehaviorDeviationProfileRepository deviations,SeasonSnapshotRepository snapshots,BehaviorEvidenceSetRepository evidence,BehaviorEvidenceAnalyzer analyzer){this.deviations=Objects.requireNonNull(deviations);this.snapshots=Objects.requireNonNull(snapshots);this.evidence=Objects.requireNonNull(evidence);this.analyzer=Objects.requireNonNull(analyzer);}
 public BehaviorEvidenceAnalysisReport rebuild(){lastReport=analyzer.analyze(deviations.findAll(),snapshots.findAll());evidence.replaceAll(lastReport.caseFiles());return lastReport;}
 public BehaviorEvidenceAnalysisReport lastReport(){return lastReport;}public Optional<BehaviorEvidenceSet> findById(BehaviorEvidenceSetId id){return evidence.findById(id);}public List<BehaviorEvidenceSet> findAll(){return evidence.findAll();}public long count(){return evidence.count();}
}
