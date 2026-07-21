package tests;

/** Regression gate for the first SEV Mid increment: M1.1 canonical event model. */
public final class MidM1_1RegressionSuite {
    private MidM1_1RegressionSuite() {}

    public static void main(String[] args) throws Exception {
        JuniorRegressionSuite.main(new String[0]);
        EconomicEventModelTest.main(new String[0]);
        System.out.println("MidM1_1RegressionSuite: ALL TESTS PASSED");
    }
}
