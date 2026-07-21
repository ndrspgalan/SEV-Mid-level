package tests;
import behavior.deviation.profile.BehaviorDeviation;
import behavior.expected.profile.PopulationStatistics;
import java.util.OptionalDouble;
public final class BehaviorDeviationValueTest{
 public static void main(String[]a){
  PopulationStatistics zeroVariance=new PopulationStatistics(2,0,2,0,0,0,0,OptionalDouble.of(0),0,0,0,0,0);
  BehaviorDeviation d=BehaviorDeviation.compare(0,zeroVariance,OptionalDouble.of(50));
  check(d.relativeDifferenceFromMean().isEmpty(),"relative difference undefined at zero mean");
  check(d.zScore().isEmpty(),"z-score undefined at zero variance");
  System.out.println("BehaviorDeviationValueTest: PASSED");
 }
 private static void check(boolean c,String m){if(!c)throw new AssertionError(m);}
}
