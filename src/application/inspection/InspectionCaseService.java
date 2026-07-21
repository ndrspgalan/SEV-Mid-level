package application.inspection;

import behavior.recommendation.profile.*;
import behavior.recommendation.repository.InspectionRecommendationRepository;
import inspection.casefile.*;
import inspection.repository.InspectionCaseRepository;
import java.time.Clock;
import java.util.*;

/** Opens persistent M4.1 cases from actionable immutable M3.7 recommendations. */
public final class InspectionCaseService {
    private final InspectionRecommendationRepository recommendations;
    private final InspectionCaseRepository cases;
    private final Clock clock;

    public InspectionCaseService(
            InspectionRecommendationRepository recommendations,
            InspectionCaseRepository cases,
            Clock clock
    ) {
        this.recommendations = Objects.requireNonNull(recommendations);
        this.cases = Objects.requireNonNull(cases);
        this.clock = Objects.requireNonNull(clock);
    }

    public InspectionCase open(InspectionRecommendationId recommendationId) {
        InspectionRecommendation recommendation = recommendations.findById(Objects.requireNonNull(recommendationId))
                .orElseThrow(() -> new NoSuchElementException("inspection recommendation not found: " + recommendationId.value()));
        return open(recommendation);
    }

    public InspectionCase open(InspectionRecommendation recommendation) {
        Objects.requireNonNull(recommendation);
        if (!recommendation.recommendsInspection()) {
            throw new IllegalArgumentException("a NONE recommendation cannot open an inspection case");
        }
        InspectionCaseId caseId = InspectionCaseId.from(recommendation.id());
        return cases.findById(caseId).orElseGet(() -> cases.save(InspectionCase.open(recommendation, clock.instant())));
    }

    /** Opens every currently actionable recommendation, preserving idempotence. */
    public List<InspectionCase> openRecommended() {
        List<InspectionCase> opened = new ArrayList<>();
        for (InspectionRecommendation recommendation : recommendations.findAll()) {
            if (recommendation.recommendsInspection()) opened.add(open(recommendation));
        }
        return List.copyOf(opened);
    }

    public Optional<InspectionCase> findById(InspectionCaseId id) { return cases.findById(id); }
    public List<InspectionCase> findAll() { return cases.findAll(); }
    public long count() { return cases.count(); }
}
