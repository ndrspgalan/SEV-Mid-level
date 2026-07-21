package inspection.casefile;

import behavior.recommendation.profile.InspectionRecommendation;
import behavior.temporal.SeasonPeriod;
import java.time.Instant;
import java.util.Objects;

/**
 * M4.1 persistent administrative case.
 *
 * The complete immutable M3.7 recommendation is retained as the origin of the
 * procedure, so its scope, reasons and analytical trace are not duplicated and
 * cannot disappear after a later analytical rebuild.
 */
public final class InspectionCase {
    private final InspectionCaseId id;
    private final InspectionRecommendation sourceRecommendation;
    private final InspectionCaseStatus status;
    private final Instant openedAt;

    private InspectionCase(
            InspectionCaseId id,
            InspectionRecommendation sourceRecommendation,
            InspectionCaseStatus status,
            Instant openedAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.sourceRecommendation = Objects.requireNonNull(sourceRecommendation);
        this.status = Objects.requireNonNull(status);
        this.openedAt = Objects.requireNonNull(openedAt);

        if (!id.equals(InspectionCaseId.from(sourceRecommendation.id()))) {
            throw new IllegalArgumentException("inspection case identity mismatch");
        }
        if (!sourceRecommendation.recommendsInspection()) {
            throw new IllegalArgumentException("a NONE recommendation cannot open an inspection case");
        }
        // OPEN and CLOSED are the complete lifecycle states of an inspection case.
    }

    public static InspectionCase open(InspectionRecommendation recommendation, Instant openedAt) {
        Objects.requireNonNull(recommendation);
        return new InspectionCase(
                InspectionCaseId.from(recommendation.id()),
                recommendation,
                InspectionCaseStatus.OPEN,
                openedAt
        );
    }

    public InspectionCase close() {
        if (!isOpen()) throw new IllegalStateException("inspection case is already closed");
        return new InspectionCase(id, sourceRecommendation, InspectionCaseStatus.CLOSED, openedAt);
    }

    public InspectionCaseId id() { return id; }
    public InspectionRecommendation sourceRecommendation() { return sourceRecommendation; }
    public InspectionCaseStatus status() { return status; }
    public Instant openedAt() { return openedAt; }
    public SeasonPeriod openedSeason() { return sourceRecommendation.seasonPeriod(); }
    public boolean isOpen() { return status == InspectionCaseStatus.OPEN; }
}
