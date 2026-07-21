package tests;

import banking.identity.*;
import banking.census.ProfessionCatalog;
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

public final class ExpectedBehaviorAnalyzerTest {
 public static void main(String[]args){
  Profession profession=ProfessionCatalog.valerianStandard().require("Jornalero");
  ConsumerId active=ConsumerId.random(), inactive=ConsumerId.random(); BankAccountId a=BankAccountId.random(), seller=BankAccountId.random();
  EconomicEvent e=purchase("M31-A","2025-11-03T10:00:00Z",active,a,seller,profession);
  ProfessionalBehaviorProfile profile=new ProfessionalBehaviorAnalyzer(new ValerianProfessionCreditProfileResolver(),new ValerianSeasonResolver(ZoneOffset.UTC),ZoneOffset.UTC).analyze(List.of(e)).get(0);
  PopulationSnapshot population=new PopulationSnapshot(profession,profile.seasonPeriod(),Set.of(active,inactive),0,0,0,0,0,0,0,0,0,0,0,0,0);
  SeasonSnapshot season=new SeasonSnapshot(profile.seasonPeriod(),Map.of(profession.code(),population));
  var report=new ExpectedBehaviorAnalyzer(new ValerianSeasonResolver(ZoneOffset.UTC)).analyze(List.of(season),List.of(profile),List.of(e));
  check(report.expectedBehaviorSets().size()==1,"one set");
  ExpectedBehaviorSet set=report.expectedBehaviorSets().get(0); check(set.populationSize()==2,"complete population");
  var metric=ExpectedBehaviorMetric.of(ExpectedBehaviorMetricFamily.SEASON_ACTIVITY_TOTAL,"ALL");
  PopulationStatistics stats=set.metrics().get(metric); check(stats.mean()==0.5,"inactive member must contribute zero"); check(stats.activeMemberCount()==1,"active count");
  var money=ExpectedBehaviorMetric.monetary(ExpectedBehaviorMetricFamily.CONSUMABLE_MONETARY_VOLUME,"FOOD-001",Currency.VALERITA);
  check(set.metrics().get(money).maximum()==3,"currency volume");
  check(report.inactiveConsumersSynthesized()==1,"inactive report");
  System.out.println("ExpectedBehaviorAnalyzerTest: PASSED");
 }
 private static EconomicEvent purchase(String id,String at,ConsumerId c,BankAccountId a,BankAccountId seller,Profession profession){EconomicEventSource source=new EconomicEventSource(EconomicEventSourceType.TRANSACTION_LEDGER,id,"Purchase");return new EconomicEvent(source.eventId(),Instant.parse(at),EconomicEventType.PURCHASE_EXECUTED,EconomicEventCategory.COMMERCIAL,EconomicEventStatus.SUCCEEDED,new EconomicActor(a,c),Optional.of(new EconomicCounterparty(seller)),Optional.of(new EconomicAmount(Currency.VALERITA,3)),Optional.empty(),Optional.of(profession),Optional.of("FOOD"),Optional.empty(),source,Map.of("consumableId","FOOD-001","consumableName","Pan","consumableCategory","FOOD","quantity","1","unitPrice","3"));}
 private static void check(boolean c,String m){if(!c)throw new AssertionError(m);}
}
