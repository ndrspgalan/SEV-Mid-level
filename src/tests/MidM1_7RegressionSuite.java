package tests;

/** Regression gate for SEV Mid M1.7 analytical auditing. */
public final class MidM1_7RegressionSuite {
    private MidM1_7RegressionSuite() {}

    public static void main(String[] args) throws Exception {
        MidM1_6RegressionSuite.main(new String[0]);
        EconomicEventInvariantAuditorTest.main(new String[0]);
        System.out.println("MidM1_7RegressionSuite: ALL TESTS PASSED");
    }
}
