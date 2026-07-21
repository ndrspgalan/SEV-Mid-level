package inspection.resolution.casefile;

import behavior.temporal.SeasonPeriod;
import inspection.casefile.InspectionCaseId;
import java.time.Instant;
import java.util.Objects;

/**
 * Final immutable M4.3 response to an inspection case.
 *
 * SEV never judges moral or legal responsibility. A resolution declares no
 * guilt, innocence, punishment, compensation or reparation. It only explains
 * why behavior cannot be adequately justified under one concrete deployment
 * and recommends how that deployment should react. AUDIT adapts deployment
 * policy. FRAUD additionally constrains the responsible economic scope. Any
 * later judicial or administrative action is external to SEV.
 */
public final class InspectionResolution {
    private final InspectionResolutionId id;
    private final InspectionCaseId inspectionCaseId;
    private final SeasonPeriod openedSeason;
    private final InspectionResolutionType type;
    private final InstitutionalActionPlan actionPlan;
    private final InspectionResolutionExplanation explanation;
    private final Instant resolvedAt;

    public InspectionResolution(
            InspectionResolutionId id,
            InspectionCaseId inspectionCaseId,
            SeasonPeriod openedSeason,
            InspectionResolutionType type,
            InstitutionalActionPlan actionPlan,
            InspectionResolutionExplanation explanation,
            Instant resolvedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.inspectionCaseId = Objects.requireNonNull(inspectionCaseId);
        this.openedSeason = Objects.requireNonNull(openedSeason);
        this.type = Objects.requireNonNull(type);
        this.actionPlan = Objects.requireNonNull(actionPlan);
        this.explanation = Objects.requireNonNull(explanation);
        this.resolvedAt = Objects.requireNonNull(resolvedAt);
        if (!id.equals(InspectionResolutionId.from(inspectionCaseId))) {
            throw new IllegalArgumentException("inspection resolution identity mismatch");
        }
        validatePlan(type, actionPlan);
    }

    private static void validatePlan(InspectionResolutionType type, InstitutionalActionPlan plan) {
        switch (type) {
            case TO_FILE -> {
                if (!plan.isEmpty()) throw new IllegalArgumentException("TO_FILE cannot contain institutional actions");
            }
            case AUDIT -> {
                if (plan.policyAdjustments().isEmpty()) {
                    throw new IllegalArgumentException("AUDIT requires at least one policy adjustment");
                }
                if (!plan.restrictiveMeasures().isEmpty()) {
                    throw new IllegalArgumentException("AUDIT cannot contain restrictive measures");
                }
            }
            case FRAUD -> {
                if (plan.policyAdjustments().isEmpty()) {
                    throw new IllegalArgumentException("FRAUD requires at least one policy adjustment");
                }
                if (plan.restrictiveMeasures().isEmpty()) {
                    throw new IllegalArgumentException("FRAUD requires at least one restrictive measure");
                }
            }
        }
    }

    public InspectionResolutionId id() { return id; }
    public InspectionCaseId inspectionCaseId() { return inspectionCaseId; }
    public SeasonPeriod openedSeason() { return openedSeason; }
    public InspectionResolutionType type() { return type; }
    public InstitutionalActionPlan actionPlan() { return actionPlan; }
    public InspectionResolutionExplanation explanation() { return explanation; }
    public Instant resolvedAt() { return resolvedAt; }
}
