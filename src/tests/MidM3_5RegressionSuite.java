package tests;

public final class MidM3_5RegressionSuite {
    private MidM3_5RegressionSuite() {}
    public static void main(String[] args) throws Exception {
        MidM3_4RegressionSuite.main(args);
        EconomicCorrelationAnalyzerTest.main(args);
        System.out.println("MidM3_5RegressionSuite: ALL TESTS PASSED");
    }
}
