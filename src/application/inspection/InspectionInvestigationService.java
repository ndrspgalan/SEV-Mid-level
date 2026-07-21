package application.inspection;

import inspection.casefile.*;
import inspection.repository.InspectionCaseRepository;
import inspection.investigation.casefile.*;
import inspection.investigation.repository.*;
import java.time.*;
import java.util.*;

/**
 * M4.2 workspace for discretionary investigation.
 * The service validates and persists inspector-provided evidence and hypotheses;
 * it never invents either of them.
 */
public final class InspectionInvestigationService {
    private final InspectionCaseRepository cases;
    private final EvidenceRecordRepository evidenceRecords;
    private final HypothesisRecordRepository hypothesisRecords;
    private final Clock clock;

    public InspectionInvestigationService(
            InspectionCaseRepository cases,
            EvidenceRecordRepository evidenceRecords,
            HypothesisRecordRepository hypothesisRecords,
            Clock clock
    ) {
        this.cases = Objects.requireNonNull(cases);
        this.evidenceRecords = Objects.requireNonNull(evidenceRecords);
        this.hypothesisRecords = Objects.requireNonNull(hypothesisRecords);
        this.clock = Objects.requireNonNull(clock);
    }

    public EvidenceRecord registerEvidence(
            InspectionCaseId caseId,
            EvidenceRecordType type,
            String title,
            String description,
            String sourceReference,
            Instant obtainedAt
    ) {
        requireOpenCase(caseId);
        EvidenceRecord record = new EvidenceRecord(
                EvidenceRecordId.generate(), caseId, type, title, description,
                sourceReference, obtainedAt, clock.instant());
        return evidenceRecords.save(record);
    }

    public HypothesisRecord registerHypothesis(InspectionCaseId caseId, String statement) {
        requireOpenCase(caseId);
        return hypothesisRecords.save(HypothesisRecord.open(
                HypothesisRecordId.generate(), caseId, statement, clock.instant()));
    }

    public HypothesisRecord relateEvidence(
            HypothesisRecordId hypothesisId,
            EvidenceRecordId evidenceId,
            HypothesisEvidenceImpact impact
    ) {
        HypothesisRecord hypothesis = requireHypothesis(hypothesisId);
        requireOpenCase(hypothesis.inspectionCaseId());
        EvidenceRecord evidence = evidenceRecords.findById(Objects.requireNonNull(evidenceId))
                .orElseThrow(() -> new NoSuchElementException("evidence record not found: " + evidenceId));
        if (!evidence.inspectionCaseId().equals(hypothesis.inspectionCaseId())) {
            throw new IllegalArgumentException("hypothesis and evidence must belong to the same inspection case");
        }
        return hypothesisRecords.save(hypothesis.relate(evidenceId, impact, clock.instant()));
    }

    public HypothesisRecord concludeHypothesis(
            HypothesisRecordId hypothesisId,
            HypothesisRecordStatus conclusion
    ) {
        HypothesisRecord hypothesis = requireHypothesis(hypothesisId);
        requireOpenCase(hypothesis.inspectionCaseId());
        return hypothesisRecords.save(hypothesis.conclude(conclusion, clock.instant()));
    }

    public List<EvidenceRecord> evidenceFor(InspectionCaseId caseId) {
        requireCase(caseId);
        return evidenceRecords.findByCaseId(caseId);
    }

    public List<HypothesisRecord> hypothesesFor(InspectionCaseId caseId) {
        requireCase(caseId);
        return hypothesisRecords.findByCaseId(caseId);
    }

    private HypothesisRecord requireHypothesis(HypothesisRecordId id) {
        return hypothesisRecords.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new NoSuchElementException("hypothesis record not found: " + id));
    }

    private InspectionCase requireCase(InspectionCaseId id) {
        return cases.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new NoSuchElementException("inspection case not found: " + id));
    }

    private InspectionCase requireOpenCase(InspectionCaseId id) {
        InspectionCase inspectionCase = requireCase(id);
        if (!inspectionCase.isOpen()) throw new IllegalStateException("inspection case is closed");
        return inspectionCase;
    }
}
