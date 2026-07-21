package behavior.evidence.analysis;

import behavior.evidence.casefile.BehaviorEvidenceSet;
import java.util.*;

public record BehaviorEvidenceAnalysisReport(long deviationProfilesExamined,long caseFilesProduced,long evidenceEntriesProduced,long missingInstitutionalContexts,long inconsistentInputs,List<BehaviorEvidenceSet> caseFiles){
    public BehaviorEvidenceAnalysisReport{
        if(deviationProfilesExamined<0||caseFilesProduced<0||evidenceEntriesProduced<0||missingInstitutionalContexts<0||inconsistentInputs<0) throw new IllegalArgumentException("negative report value");
        caseFiles=List.copyOf(Objects.requireNonNull(caseFiles));
    }
}
