package tests;

public final class MidM1_8RegressionSuite {
    private MidM1_8RegressionSuite() {}
    public static void main(String[] args) throws Exception {
        MidM1_7RegressionSuite.main(args);
        MidM1_8ConsoleTest.main(args);
        System.out.println("MidM1_8RegressionSuite: ALL TESTS PASSED");
    }
}
