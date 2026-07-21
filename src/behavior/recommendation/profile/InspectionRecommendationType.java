package behavior.recommendation.profile;

/** Institutional scope of the recommended action. The scope itself defines urgency. */
public enum InspectionRecommendationType {
    NONE(0),
    INDIVIDUAL(1),
    GROUP(2),
    PROFESSION(3),
    SYSTEMIC(4);

    private final int institutionalRank;

    InspectionRecommendationType(int institutionalRank) {
        this.institutionalRank = institutionalRank;
    }

    public int institutionalRank() {
        return institutionalRank;
    }

    public static InspectionRecommendationType mostExtensive(
            InspectionRecommendationType first,
            InspectionRecommendationType second
    ) {
        if (first == null || second == null) throw new IllegalArgumentException("recommendation type");
        return first.institutionalRank >= second.institutionalRank ? first : second;
    }
}
