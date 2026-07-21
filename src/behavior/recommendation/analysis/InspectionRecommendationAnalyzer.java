package behavior.recommendation.analysis;

import behavior.health.profile.*;
import behavior.recommendation.profile.*;
import java.util.*;

/** Converts M3.6 assessments into institutional scopes without opening a procedure. */
public final class InspectionRecommendationAnalyzer {

    public InspectionRecommendationReport analyze(Collection<EconomicHealthAssessment> assessments) {
        Objects.requireNonNull(assessments);
        List<InspectionRecommendation> recommendations = assessments.stream()
                .sorted(Comparator.comparing(a -> a.seasonPeriod().startsOn()))
                .map(this::recommend)
                .toList();
        return new InspectionRecommendationReport(
                assessments.size(),
                recommendations.size(),
                count(recommendations, InspectionRecommendationType.NONE),
                count(recommendations, InspectionRecommendationType.INDIVIDUAL),
                count(recommendations, InspectionRecommendationType.GROUP),
                count(recommendations, InspectionRecommendationType.PROFESSION),
                count(recommendations, InspectionRecommendationType.SYSTEMIC),
                recommendations);
    }

    private InspectionRecommendation recommend(EconomicHealthAssessment assessment) {
        List<EconomicHealthObservation> concerning = assessment.observations().stream()
                .filter(o -> o.indicator() == EconomicHealthStatus.CONCERNING)
                .toList();

        InspectionRecommendationType type;
        EnumSet<InspectionRecommendationReason> reasons = EnumSet.noneOf(InspectionRecommendationReason.class);
        Collection<EconomicHealthObservation> supporting;
        String summary;

        if (concerning.isEmpty()) {
            type = InspectionRecommendationType.NONE;
            supporting = assessment.observations();
            if (assessment.status() == EconomicHealthStatus.STABLE) {
                reasons.add(InspectionRecommendationReason.STABLE_ECONOMY);
                summary = "La evaluación económica es estable y no justifica iniciar una Inspección.";
            } else {
                reasons.add(InspectionRecommendationReason.INSUFFICIENT_EVIDENCE);
                summary = "La evaluación no contiene una conclusión preocupante suficiente para recomendar una Inspección.";
            }
        } else {
            type = concerning.stream()
                    .map(EconomicHealthObservation::scope)
                    .map(InspectionRecommendationAnalyzer::typeFromScope)
                    .reduce(InspectionRecommendationType.NONE, InspectionRecommendationType::mostExtensive);
            concerning.stream().map(EconomicHealthObservation::type)
                    .map(InspectionRecommendationAnalyzer::reasonFromObservation)
                    .forEach(reasons::add);
            supporting = concerning;
            summary = "Las observaciones preocupantes justifican una recomendación de Inspección con alcance " + type.name() + ".";
        }

        return new InspectionRecommendation(
                InspectionRecommendationId.from(assessment.id()),
                assessment.id(),
                assessment.seasonPeriod(),
                type,
                reasons,
                explanation(summary, supporting));
    }

    private static InspectionRecommendationExplanation explanation(
            String summary,
            Collection<EconomicHealthObservation> observations
    ) {
        Set<EconomicHealthObservationId> observationIds = new LinkedHashSet<>();
        var alignments = new LinkedHashSet<behavior.alignment.profile.StructuralAlignmentId>();
        var correlations = new LinkedHashSet<behavior.correlation.profile.EconomicCorrelationId>();
        Set<String> professions = new TreeSet<>();
        Set<String> profiles = new TreeSet<>();
        for (EconomicHealthObservation observation : observations) {
            observationIds.add(observation.id());
            alignments.addAll(observation.explanation().alignmentEvidence());
            correlations.addAll(observation.explanation().correlationEvidence());
            professions.addAll(observation.explanation().professions());
            profiles.addAll(observation.explanation().institutionalProfiles());
        }
        return new InspectionRecommendationExplanation(summary, observationIds, alignments, correlations, professions, profiles);
    }

    private static InspectionRecommendationType typeFromScope(ObservationScope scope) {
        return switch (scope) {
            case INDIVIDUAL -> InspectionRecommendationType.INDIVIDUAL;
            case GROUP -> InspectionRecommendationType.GROUP;
            case PROFESSION -> InspectionRecommendationType.PROFESSION;
            case SYSTEM -> InspectionRecommendationType.SYSTEMIC;
        };
    }

    private static InspectionRecommendationReason reasonFromObservation(EconomicHealthObservationType type) {
        return switch (type) {
            case CREDIT_PROFILE_MIGRATION -> InspectionRecommendationReason.CREDIT_PROFILE_MIGRATION;
            case PROFESSIONAL_CONVERGENCE -> InspectionRecommendationReason.PROFESSIONAL_CONVERGENCE;
            case PROFESSIONAL_DIVERGENCE -> InspectionRecommendationReason.PROFESSIONAL_DIVERGENCE;
            case MIRROR_MOVEMENT -> InspectionRecommendationReason.MIRROR_MOVEMENT;
            case SYSTEMIC_ALIGNMENT -> InspectionRecommendationReason.SYSTEMIC_ALIGNMENT;
            case STABLE_ALIGNMENT, ISOLATED_ALIGNMENT, INSUFFICIENT_EVIDENCE ->
                    throw new IllegalArgumentException("non-concerning observation cannot justify inspection: " + type);
        };
    }

    private static long count(List<InspectionRecommendation> recommendations, InspectionRecommendationType type) {
        return recommendations.stream().filter(r -> r.type() == type).count();
    }
}
