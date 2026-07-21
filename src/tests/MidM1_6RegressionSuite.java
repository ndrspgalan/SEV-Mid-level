package tests;

/** Regression gate for SEV Mid M1.6 analytical projection. */
public final class MidM1_6RegressionSuite {
    private MidM1_6RegressionSuite() {}

    public static void main(String[] args) throws Exception {
        MidM1_5RegressionSuite.main(new String[0]);
        CompositeEconomicEventNormalizerTest.main(new String[0]);
        EconomicEventProjectionServiceTest.main(new String[0]);
        System.out.println("MidM1_6RegressionSuite: ALL TESTS PASSED");
    }
}
