package tests;
import behavior.expected.analysis.PopulationStatisticsCalculator;
import java.util.List;
public final class PopulationStatisticsCalculatorTest{public static void main(String[]a){var s=new PopulationStatisticsCalculator().calculate(List.of(0d,0d,2d,6d));if(s.mean()!=2d||s.median()!=1d||s.percentile25()!=0d||s.percentile75()!=3d||s.interquartileRange()!=3d||s.activeMemberCount()!=2)throw new AssertionError("population statistics");System.out.println("PopulationStatisticsCalculatorTest: PASSED");}}
