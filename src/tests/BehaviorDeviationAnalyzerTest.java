package tests;

import banking.identity.*;
import banking.census.ProfessionCatalog;
import behavior.deviation.analysis.*;
import behavior.deviation.profile.*;
import behavior.expected.analysis.*;
import behavior.expected.profile.*;
import behavior.temporal.*;
import behavior.temporal.analysis.ProfessionalBehaviorAnalyzer;
import behavior.temporal.profile.ProfessionalBehaviorProfile;
import coinProperties.Currency;
import economicEvent.*;
import institutional.snapshot.*;
import operationalControl.profile.ValerianProfessionCreditProfileResolver;
import java.time.*;
import java.util.*;

public final class BehaviorDeviationAnalyzerTest {
 public static void main(String[]args){
  Profession profession=ProfessionCatalog.valerianStandard().require("Jornalero");
  ConsumerId active=ConsumerId.random(), inactive=ConsumerId.random(); BankAccountId a=BankAccountId.random(), seller=BankAccountId.random();
  EconomicEvent event=purchase("M32-A","2025-11-03T10:00:00Z",active,a,seller,profession);
  ValerianSeasonResolver resolver=new ValerianSeasonResolver(ZoneOffset.UTC);
  ProfessionalBehaviorProfile profile=new ProfessionalBehaviorAnalyzer(new ValerianProfessionCreditProfileResolver(),resolver,ZoneOffset.UTC).analyze(List.of(event)).get(0);
  PopulationSnapshot population=new PopulationSnapshot(profession,profile.seasonPeriod(),Set.of(active,inactive),0,0,0,0,0,0,0,0,0,0,0,0,0);
  SeasonSnapshot season=new SeasonSnapshot(profile.seasonPeriod(),Map.of(profession.code(),population));
  ExpectedBehaviorSet expected=new ExpectedBehaviorAnalyzer(resolver).analyze(List.of(season),List.of(profile),List.of(event)).expectedBehaviorSets().get(0);
  BehaviorDeviationAnalysisReport report=new BehaviorDeviationAnalyzer(resolver).analyze(List.of(season),List.of(profile),List.of(expected),List.of(event));
  check(report.deviationProfiles().size()==2,"one comparison per population member");
  ExpectedBehaviorMetric metric=ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.SEASON_ACTIVITY_TOTAL,"ALL");
  BehaviorDeviation activeDeviation=find(report,active).deviations().get(metric);
  BehaviorDeviation inactiveDeviation=find(report,inactive).deviations().get(metric);
  check(activeDeviation.observedValue()==1d,"active observation");
  check(activeDeviation.signedDifferenceFromMean()==0.5d,"positive signed difference");
  check(activeDeviation.zScore().isPresent()&&activeDeviation.zScore().getAsDouble()==1d,"active z-score");
  check(activeDeviation.percentileRank().isPresent()&&activeDeviation.percentileRank().getAsDouble()==75d,"active midrank percentile");
  check(inactiveDeviation.observedValue()==0d,"synthesized inactive observation");
  check(inactiveDeviation.zScore().isPresent()&&inactiveDeviation.zScore().getAsDouble()==-1d,"inactive z-score");
  check(inactiveDeviation.percentileRank().isPresent()&&inactiveDeviation.percentileRank().getAsDouble()==25d,"inactive midrank percentile");
  check(report.inactiveMembersSynthesized()==1,"inactive report");
  System.out.println("BehaviorDeviationAnalyzerTest: PASSED");
 }
 private static BehaviorDeviationProfile find(BehaviorDeviationAnalysisReport report,ConsumerId id){return report.deviationProfiles().stream().filter(p->p.consumerId().equals(id)).findFirst().orElseThrow();}
 private static EconomicEvent purchase(String id,String at,ConsumerId c,BankAccountId a,BankAccountId seller,Profession profession){EconomicEventSource source=new EconomicEventSource(EconomicEventSourceType.TRANSACTION_LEDGER,id,"Purchase");return new EconomicEvent(source.eventId(),Instant.parse(at),EconomicEventType.PURCHASE_EXECUTED,EconomicEventCategory.COMMERCIAL,EconomicEventStatus.SUCCEEDED,new EconomicActor(a,c),Optional.of(new EconomicCounterparty(seller)),Optional.of(new EconomicAmount(Currency.VALERITA,3)),Optional.empty(),Optional.of(profession),Optional.of("FOOD"),Optional.empty(),source,Map.of("consumableId","FOOD-001","consumableName","Pan","consumableCategory","FOOD","quantity","1","unitPrice","3"));}
 private static void check(boolean c,String m){if(!c)throw new AssertionError(m);}
}
