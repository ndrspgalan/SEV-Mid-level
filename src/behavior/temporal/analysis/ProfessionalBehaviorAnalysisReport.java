package behavior.temporal.analysis;

import behavior.temporal.profile.ProfessionalBehaviorProfile;

import java.util.List;
import java.util.Objects;

/** Auditable result of rebuilding the profession-contextualized projection. */
public record ProfessionalBehaviorAnalysisReport(
        long examinedEvents,
        long omittedWithoutHistoricalProfession,
        List<ProfessionalBehaviorProfile> profiles
) {
    public ProfessionalBehaviorAnalysisReport {
        if (examinedEvents < 0 || omittedWithoutHistoricalProfession < 0 || omittedWithoutHistoricalProfession > examinedEvents)
            throw new IllegalArgumentException("invalid analysis counters");
        profiles = List.copyOf(Objects.requireNonNull(profiles));
    }
}
