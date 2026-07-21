package tests;

import banking.census.ProfessionCatalog;
import banking.identity.*;
import behavior.deviation.profile.*;
import behavior.evidence.analysis.*;
import behavior.evidence.casefile.*;
import behavior.expected.profile.*;
import behavior.temporal.*;
import institutional.snapshot.*;
import java.time.*;
import java.util.*;

public final class BehaviorEvidenceAnalyzerTest {
 public static void main(String[]args){
  Profession profession=ProfessionCatalog.valerianStandard().require("Jornalero");
  ConsumerId consumer=ConsumerId.random();
  ValerianSeasonResolver resolver=new ValerianSeasonResolver(ZoneOffset.UTC);
  SeasonPeriod previous=resolver.resolve(Instant.parse("2025-08-03T10:00:00Z"));
  SeasonPeriod current=resolver.resolve(Instant.parse("2025-11-03T10:00:00Z"));
  ExpectedBehaviorSetId expectedId=ExpectedBehaviorSetId.of(profession.code(),current);
  ExpectedBehaviorMetric activity=ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.SEASON_ACTIVITY_TOTAL,"ALL");
  ExpectedBehaviorMetric night=ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.DAY_PERIOD_ACTIVITY,"NIGHT");
  ExpectedBehaviorMetric transfers=ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.TRANSFERS_SENT,"FUNDS_TRANSFERRED");
  Map<ExpectedBehaviorMetric,BehaviorDeviation> deviations=new TreeMap<>();
  deviations.put(activity,new BehaviorDeviation(8,4,3,4,4,5,OptionalDouble.of(1),OptionalDouble.of(90),OptionalDouble.of(2)));
  deviations.put(night,new BehaviorDeviation(0,1,1,1,-1,-1,OptionalDouble.of(-1),OptionalDouble.of(20),OptionalDouble.of(-1)));
  deviations.put(transfers,new BehaviorDeviation(2,2,2,0,0,0,OptionalDouble.of(0),OptionalDouble.of(50),OptionalDouble.empty()));
  BehaviorDeviationProfile profile=new BehaviorDeviationProfile(BehaviorDeviationProfileId.of(consumer,profession.code(),current),consumer,profession,current,expectedId,deviations);
  PopulationSnapshot previousPopulation=new PopulationSnapshot(profession,previous,Set.of(consumer),0,0,0,0,0,0,0,3,2,0,0,0,0);
  PopulationSnapshot currentPopulation=new PopulationSnapshot(profession,current,Set.of(consumer),1,0,0,1,0,0,0,8,5,1,0,0,0);
  SeasonSnapshot previousSnapshot=new SeasonSnapshot(previous,Map.of(profession.code(),previousPopulation));
  SeasonSnapshot currentSnapshot=new SeasonSnapshot(current,Map.of(profession.code(),currentPopulation));
  BehaviorEvidenceAnalysisReport report=new BehaviorEvidenceAnalyzer().analyze(List.of(profile),List.of(previousSnapshot,currentSnapshot));
  check(report.caseFilesProduced()==1,"one case file");check(report.evidenceEntriesProduced()==3,"all deviations preserved");
  BehaviorEvidenceSet set=report.caseFiles().get(0);
  check(set.evidenceCount()==3,"case file evidence count");
  check(set.evidenceByCategory().get(BehaviorEvidenceCategory.ECONOMIC_ACTIVITY).size()==1,"activity category");
  check(set.evidenceByCategory().get(BehaviorEvidenceCategory.TEMPORAL_ACTIVITY).size()==1,"temporal category");
  check(set.evidenceByCategory().get(BehaviorEvidenceCategory.TRANSFER_NETWORK).size()==1,"network category");
  BehaviorEvidence activityEvidence=set.evidenceByCategory().get(BehaviorEvidenceCategory.ECONOMIC_ACTIVITY).get(0);
  check(activityEvidence.direction()==DeviationDirection.ABOVE_REFERENCE,"direction preserved");
  check(activityEvidence.standardizedMagnitude().orElseThrow()==2d,"absolute z magnitude");
  check(activityEvidence.percentileExtremity().orElseThrow()==40d,"percentile extremity");
  check(set.institutionalContext().populationDeltaFromPreviousSeason().orElseThrow()==0,"population delta");
  check(set.institutionalContext().transferBalanceDeltaFromPreviousSeason().orElseThrow()==-2,"transfer balance delta");
  System.out.println("BehaviorEvidenceAnalyzerTest: PASSED");
 }
 private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
