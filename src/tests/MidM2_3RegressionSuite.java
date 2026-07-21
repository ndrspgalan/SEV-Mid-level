package tests;
public final class MidM2_3RegressionSuite {
    private MidM2_3RegressionSuite(){}
    public static void main(String[] args) throws Exception {
        MidM2_2RegressionSuite.main(args);
        TemporalStatisticsCalculatorTest.main(args);
        ProfessionalBehaviorAnalyzerTest.main(args);
        ProfessionalBehaviorProfileServiceTest.main(args);
        System.out.println("MidM2_3RegressionSuite: ALL TESTS PASSED");
    }
}
