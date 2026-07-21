package behavior.deviation.analysis;

import behavior.deviation.profile.BehaviorDeviationProfile;
import java.util.*;

public record BehaviorDeviationAnalysisReport(
        long expectedBehaviorSetsExamined,
        long populationMembersExamined,
        long behaviorProfilesResolved,
        long inactiveMembersSynthesized,
        long inconsistentProfiles,
        long deviationsProduced,
        List<BehaviorDeviationProfile> deviationProfiles
) {
    public BehaviorDeviationAnalysisReport {
        if (expectedBehaviorSetsExamined < 0 || populationMembersExamined < 0 || behaviorProfilesResolved < 0 ||
                inactiveMembersSynthesized < 0 || inconsistentProfiles < 0 || deviationsProduced < 0)
            throw new IllegalArgumentException("negative report value");
        deviationProfiles = List.copyOf(Objects.requireNonNull(deviationProfiles));
    }
}
