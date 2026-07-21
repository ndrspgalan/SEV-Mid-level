package tests;

public final class MidM5_3RegressionSuite {
    public static void main(String[] args) throws Exception {
        MidM5_2RegressionSuite.main(args);
        DoctrineEvolutionServiceTest.main(args);
        System.out.println("MidM5_3RegressionSuite: ALL TESTS PASSED");
    }
}
