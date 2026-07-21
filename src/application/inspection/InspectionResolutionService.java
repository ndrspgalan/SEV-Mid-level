package application.inspection;

import inspection.casefile.*;
import inspection.repository.InspectionCaseRepository;
import inspection.investigation.casefile.*;
import inspection.investigation.repository.*;
import inspection.resolution.casefile.*;
import inspection.resolution.repository.InspectionResolutionRepository;
import java.time.Clock;
import java.util.*;

/**
 * M4.3 closes an inspection case with an inspector-authored institutional response.
 *
 * This service never decides whether a case is TO_FILE, AUDIT or FRAUD. It only
 * validates traceability and the universal resolution invariants, persists the
 * immutable resolution and closes the corresponding case atomically within the
 * in-memory model.
 */
public final class InspectionResolutionService {
    private final InspectionCaseRepository cases;
    private final EvidenceRecordRepository evidenceRecords;
    private final HypothesisRecordRepository hypothesisRecords;
    private final InspectionResolutionRepository resolutions;
    private final JurisprudentialComparisonService jurisprudentialComparisons;
    private final Clock clock;

    public InspectionResolutionService(
            InspectionCaseRepository cases,
            EvidenceRecordRepository evidenceRecords,
            HypothesisRecordRepository hypothesisRecords,
            InspectionResolutionRepository resolutions,
            JurisprudentialComparisonService jurisprudentialComparisons,
            Clock clock
    ) {
        this.cases = Objects.requireNonNull(cases);
        this.evidenceRecords = Objects.requireNonNull(evidenceRecords);
        this.hypothesisRecords = Objects.requireNonNull(hypothesisRecords);
        this.resolutions = Objects.requireNonNull(resolutions);
        this.jurisprudentialComparisons = Objects.requireNonNull(jurisprudentialComparisons);
        this.clock = Objects.requireNonNull(clock);
    }

    public InspectionResolution resolve(
            InspectionCaseId caseId,
            InspectionResolutionType type,
            InstitutionalActionPlan actionPlan,
            InspectionResolutionExplanation explanation
    ) {
        Objects.requireNonNull(caseId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(actionPlan);
        Objects.requireNonNull(explanation);

        InspectionCase inspectionCase = requireOpenCase(caseId);
        if (resolutions.findByCaseId(caseId).isPresent()) {
            throw new IllegalStateException("inspection case already has a final resolution");
        }
        validateTraceability(caseId, explanation);

        InspectionResolution resolution = new InspectionResolution(
                InspectionResolutionId.from(caseId),
                caseId,
                inspectionCase.openedSeason(),
                type,
                actionPlan,
                explanation,
                clock.instant());

        // M5.1 must observe only earlier CLOSED precedents. The current candidate
        // remains open until its immutable jurisprudential comparison is recorded.
        jurisprudentialComparisons.compareBeforeClosing(inspectionCase, type);
        InspectionResolution stored = resolutions.save(resolution);
        cases.save(inspectionCase.close());
        return stored;
    }

    public Optional<InspectionResolution> findByCaseId(InspectionCaseId caseId) {
        return resolutions.findByCaseId(Objects.requireNonNull(caseId));
    }

    public List<InspectionResolution> findAll() { return resolutions.findAll(); }
    public long count() { return resolutions.count(); }

    private void validateTraceability(
            InspectionCaseId caseId,
            InspectionResolutionExplanation explanation
    ) {
        for (EvidenceRecordId evidenceId : explanation.supportingEvidence()) {
            EvidenceRecord evidence = evidenceRecords.findById(evidenceId)
                    .orElseThrow(() -> new NoSuchElementException("evidence record not found: " + evidenceId));
            if (!evidence.inspectionCaseId().equals(caseId)) {
                throw new IllegalArgumentException("resolution evidence must belong to the resolved inspection case");
            }
        }
        for (HypothesisRecordId hypothesisId : explanation.consideredHypotheses()) {
            HypothesisRecord hypothesis = hypothesisRecords.findById(hypothesisId)
                    .orElseThrow(() -> new NoSuchElementException("hypothesis record not found: " + hypothesisId));
            if (!hypothesis.inspectionCaseId().equals(caseId)) {
                throw new IllegalArgumentException("resolution hypothesis must belong to the resolved inspection case");
            }
        }
    }

    private InspectionCase requireOpenCase(InspectionCaseId id) {
        InspectionCase inspectionCase = cases.findById(id)
                .orElseThrow(() -> new NoSuchElementException("inspection case not found: " + id));
        if (!inspectionCase.isOpen()) throw new IllegalStateException("inspection case is closed");
        return inspectionCase;
    }
}
