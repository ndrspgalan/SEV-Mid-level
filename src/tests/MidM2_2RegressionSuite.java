package tests;

public final class MidM2_2RegressionSuite {
    private MidM2_2RegressionSuite() {}

    public static void main(String[] args) throws Exception {
        MidM2_1RegressionSuite.main(args);
        BehaviorAggregatorTest.main(args);
        BehaviorProfileServiceTest.main(args);
        System.out.println("MidM2_2RegressionSuite: ALL TESTS PASSED");
    }
}
