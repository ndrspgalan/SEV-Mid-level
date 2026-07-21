package tests;
public final class MidM2_4RegressionSuite {
 private MidM2_4RegressionSuite(){}
 public static void main(String[]args)throws Exception{
  MidM2_3RegressionSuite.main(args);
  InstitutionalSnapshotAnalyzerTest.main(args);
  EconomicHealthAnalyzerTest.main(args);
  System.out.println("MidM2_4RegressionSuite: ALL TESTS PASSED");
 }
}
