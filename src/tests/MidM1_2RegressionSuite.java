package tests;

/** Regression gate for SEV Mid M1.2 transaction normalization. */
public final class MidM1_2RegressionSuite {
    private MidM1_2RegressionSuite() {}

    public static void main(String[] args) throws Exception {
        MidM1_1RegressionSuite.main(new String[0]);
        TransactionEconomicEventNormalizerTest.main(new String[0]);
        System.out.println("MidM1_2RegressionSuite: ALL TESTS PASSED");
    }
}
