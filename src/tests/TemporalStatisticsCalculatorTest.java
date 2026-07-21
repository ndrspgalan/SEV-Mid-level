package tests;
import behavior.temporal.*;
import behavior.temporal.analysis.TemporalStatisticsCalculator;
import behavior.temporal.profile.WindowStatistics;
import java.time.*;
import java.util.*;
public final class TemporalStatisticsCalculatorTest {
    private TemporalStatisticsCalculatorTest(){}
    public static void main(String[] args){
        TemporalStatisticsCalculator c=new TemporalStatisticsCalculator(ZoneOffset.UTC);
        SeasonPeriod season=new SeasonPeriod(Season.WINTER,2026,LocalDate.of(2025,11,1),LocalDate.of(2026,1,31));
        var result=c.calculate(List.of(Instant.parse("2025-11-01T01:00:00Z"),Instant.parse("2025-11-01T09:00:00Z"),Instant.parse("2025-11-08T09:00:00Z")),season);
        WindowStatistics daily=result.at(ObservationWindow.SAME_DAY);
        check(daily.totalOccurrences()==3,"seasonal total preserved across daily buckets");
        check(daily.maximum()==2,"daily maximum");
        check(daily.standardDeviation()>0,"daily deviation");
        check(result.at(ObservationWindow.EIGHT_HOUR_PERIOD).bucketCount()==276,"three slots per winter day");
        check(result.byWindow().size()==4,"season is a summary, not a fake statistical bucket");
        System.out.println("TemporalStatisticsCalculatorTest: PASSED");
    }
    private static void check(boolean c,String m){if(!c)throw new AssertionError(m);}
}
