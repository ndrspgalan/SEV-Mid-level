package tests;

/** Regression gate for SEV Mid M1.4 enriched operational decisions. */
public final class MidM1_4RegressionSuite {
    private MidM1_4RegressionSuite() {}

    public static void main(String[] args) throws Exception {
        MidM1_3RegressionSuite.main(new String[0]);
        OperationalDecisionEconomicEventNormalizerTest.main(new String[0]);
        System.out.println("MidM1_4RegressionSuite: ALL TESTS PASSED");
    }
}
