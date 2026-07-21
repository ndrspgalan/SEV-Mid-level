package behavior.health.analysis;

import behavior.health.profile.*;
import java.util.*;

public record EconomicHealthAssessmentReport(
        long graphsExamined,
        long assessmentsProduced,
        long observationsProduced,
        long stableAssessments,
        long concerningAssessments,
        long unknownAssessments,
        List<EconomicHealthAssessment> assessments
) {
    public EconomicHealthAssessmentReport {
        if (graphsExamined < 0 || assessmentsProduced < 0 || observationsProduced < 0 || stableAssessments < 0 || concerningAssessments < 0 || unknownAssessments < 0) throw new IllegalArgumentException("negative report value");
        assessments = List.copyOf(Objects.requireNonNull(assessments));
    }
}
