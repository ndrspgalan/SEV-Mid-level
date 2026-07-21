package tests;

public final class MidM2_1RegressionSuite {
    private MidM2_1RegressionSuite() {}

    public static void main(String[] args) throws Exception {
        MidM1_8RegressionSuite.main(args);
        ConsumableCatalogM2_1Test.main(args);
        System.out.println("MidM2_1RegressionSuite: ALL TESTS PASSED");
    }
}
