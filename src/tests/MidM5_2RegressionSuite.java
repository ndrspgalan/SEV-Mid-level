package tests;

public final class MidM5_2RegressionSuite {
    public static void main(String[] args) throws Exception {
        MidM5_1RegressionSuite.main(args);
        SeasonalDoctrineUnificationServiceTest.main(args);
        System.out.println("MidM5_2RegressionSuite: ALL TESTS PASSED");
    }
}
