package coinProperties;

public enum Material {

    COPPER(1.0, 0.0, 0.0, 0.01),
    SILVER_COPPER_ALLOY(0.67, 0.33, 0.0, 0.01),
    SILVER(0.0, 1.0, 0.0, 0.01),
    GOLD(0.0, 0.0, 1.0, 0.01);

    private static final double COMPLETE_COMPOSITION = 1.0;
    private static final double FLOATING_POINT_EPSILON = 0.000_000_001;

    private final double copperRatio;
    private final double silverRatio;
    private final double goldRatio;
    private final double toleranceRatio;

    Material(
            double copperRatio,
            double silverRatio,
            double goldRatio,
            double toleranceRatio
    ) {
        validateExpectedComposition(
                copperRatio,
                silverRatio,
                goldRatio,
                toleranceRatio
        );

        this.copperRatio = copperRatio;
        this.silverRatio = silverRatio;
        this.goldRatio = goldRatio;
        this.toleranceRatio = toleranceRatio;
    }

    public boolean matchesCopperRatio(double measuredCopperRatio) {
        return isWithinExpectedTolerance(measuredCopperRatio, copperRatio);
    }

    public boolean matchesSilverRatio(double measuredSilverRatio) {
        return isWithinExpectedTolerance(measuredSilverRatio, silverRatio);
    }

    public boolean matchesGoldRatio(double measuredGoldRatio) {
        return isWithinExpectedTolerance(measuredGoldRatio, goldRatio);
    }

    public boolean matchesComposition(
            double measuredCopperRatio,
            double measuredSilverRatio,
            double measuredGoldRatio
    ) {
        return isValidMeasuredRatio(measuredCopperRatio)
                && isValidMeasuredRatio(measuredSilverRatio)
                && isValidMeasuredRatio(measuredGoldRatio)
                && formsCompleteComposition(
                        measuredCopperRatio,
                        measuredSilverRatio,
                        measuredGoldRatio
                )
                && matchesCopperRatio(measuredCopperRatio)
                && matchesSilverRatio(measuredSilverRatio)
                && matchesGoldRatio(measuredGoldRatio);
    }

    private boolean isWithinExpectedTolerance(
            double measuredRatio,
            double expectedRatio
    ) {
        if (!isValidMeasuredRatio(measuredRatio)) {
            return false;
        }

        return Math.abs(measuredRatio - expectedRatio)
                <= toleranceRatio + FLOATING_POINT_EPSILON;
    }

    private static boolean isValidMeasuredRatio(double measuredRatio) {
        return Double.isFinite(measuredRatio)
                && measuredRatio >= 0.0
                && measuredRatio <= COMPLETE_COMPOSITION;
    }

    private static boolean formsCompleteComposition(
            double measuredCopperRatio,
            double measuredSilverRatio,
            double measuredGoldRatio
    ) {
        double measuredTotal = measuredCopperRatio
                + measuredSilverRatio
                + measuredGoldRatio;

        return Math.abs(measuredTotal - COMPLETE_COMPOSITION)
                <= FLOATING_POINT_EPSILON;
    }

    private static void validateExpectedComposition(
            double copperRatio,
            double silverRatio,
            double goldRatio,
            double toleranceRatio
    ) {
        if (!isValidMeasuredRatio(copperRatio)
                || !isValidMeasuredRatio(silverRatio)
                || !isValidMeasuredRatio(goldRatio)) {
            throw new IllegalArgumentException(
                    "Expected material ratios must be finite values between 0 and 1"
            );
        }

        if (!Double.isFinite(toleranceRatio)
                || toleranceRatio < 0.0
                || toleranceRatio > COMPLETE_COMPOSITION) {
            throw new IllegalArgumentException(
                    "Material tolerance must be a finite value between 0 and 1"
            );
        }

        if (!formsCompleteComposition(copperRatio, silverRatio, goldRatio)) {
            throw new IllegalArgumentException(
                    "Expected material ratios must form one complete composition"
            );
        }
    }
}
