package behavior.expected.analysis;

import behavior.expected.profile.ExpectedBehaviorSet;
import java.util.*;

/** Transparent rebuild report; inactivity is distinguished from unresolved data. */
public record ExpectedBehaviorAnalysisReport(
        long populationSnapshotsExamined,
        long registeredConsumersExamined,
        long behaviorProfilesResolved,
        long inactiveConsumersSynthesized,
        long inconsistentProfiles,
        long metricsProduced,
        List<ExpectedBehaviorSet> expectedBehaviorSets
) {
    public ExpectedBehaviorAnalysisReport {
        long[] values={populationSnapshotsExamined,registeredConsumersExamined,behaviorProfilesResolved,inactiveConsumersSynthesized,inconsistentProfiles,metricsProduced};
        for(long value:values)if(value<0)throw new IllegalArgumentException("negative analysis counter");
        expectedBehaviorSets=List.copyOf(Objects.requireNonNull(expectedBehaviorSets));
    }
}
