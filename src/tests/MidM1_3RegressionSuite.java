package tests;

/** Regression gate for SEV Mid M1.3 account-history normalization. */
public final class MidM1_3RegressionSuite {
    private MidM1_3RegressionSuite() {}

    public static void main(String[] args) throws Exception {
        MidM1_2RegressionSuite.main(new String[0]);
        AccountHistoryEconomicEventNormalizerTest.main(new String[0]);
        System.out.println("MidM1_3RegressionSuite: ALL TESTS PASSED");
    }
}
