package tests;
import banking.census.ProfessionCatalog;
import banking.identity.*;
import behavior.temporal.*;
import institutional.analysis.EconomicHealthAnalyzer;
import institutional.snapshot.*;
import java.time.LocalDate;
import java.util.*;
public final class EconomicHealthAnalyzerTest{
 public static void main(String[]args){var p=ProfessionCatalog.valerianStandard().require("Cantero");var c1=ConsumerId.random();var c2=ConsumerId.random();
  SeasonPeriod q1=new SeasonPeriod(Season.WINTER,2026,LocalDate.of(2025,11,1),LocalDate.of(2026,1,31));
  SeasonPeriod q2=new SeasonPeriod(Season.SPRING,2026,LocalDate.of(2026,2,1),LocalDate.of(2026,4,30));
  PopulationSnapshot a=pop(p,q1,Set.of(c1),1,0),b=pop(p,q2,Set.of(c1,c2),3,1);
  var health=new EconomicHealthAnalyzer().analyze(List.of(new SeasonSnapshot(q1,Map.of(p.code(),a)),new SeasonSnapshot(q2,Map.of(p.code(),b))));
  if(health.professionEvolution().get(0).populationDelta()!=1)throw new AssertionError("population delta");
  if(health.comparisonTable().size()!=2)throw new AssertionError("table");
  System.out.println("EconomicHealthAnalyzerTest: PASSED");}
 private static PopulationSnapshot pop(Profession p,SeasonPeriod s,Set<ConsumerId> ids,long tr,long deaths){return new PopulationSnapshot(p,s,ids,0,0,0,0,0,deaths,0,tr,0,0,0,0,0);}
}
