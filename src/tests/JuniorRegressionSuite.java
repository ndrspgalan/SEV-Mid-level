package tests;

/** Single executable regression gate for the frozen SEV Junior baseline. */
public final class JuniorRegressionSuite {
    private JuniorRegressionSuite() {}

    public static void main(String[] args) throws Exception {
        run(TransactionQueryServiceTest.class);
        run(TransferOperationServiceTest.class);
        run(BankIdentityAndCensusTest.class);
        run(AccountHistoryTest.class);
        run(AccountLifecycleTest.class);
        run(OperationalControlTest.class);
        run(ProfessionCreditProfilesTest.class);
        run(JuniorArchitectureAndInvariantTest.class);
        System.out.println("JuniorRegressionSuite: ALL TESTS PASSED");
    }

    private static void run(Class<?> testClass) throws Exception {
        testClass.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
    }
}
