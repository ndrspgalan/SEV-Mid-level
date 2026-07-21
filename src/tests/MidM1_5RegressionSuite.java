package tests;

/** Regression gate for SEV Mid M1.5 analytical repository, queries and statistics. */
public final class MidM1_5RegressionSuite {
    private MidM1_5RegressionSuite() {}

    public static void main(String[] args) throws Exception {
        MidM1_4RegressionSuite.main(new String[0]);
        EconomicEventRepositoryAndQueryTest.main(new String[0]);
        System.out.println("MidM1_5RegressionSuite: ALL TESTS PASSED");
    }
}
