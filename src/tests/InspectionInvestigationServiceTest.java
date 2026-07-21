package tests;

import application.ValerianEconomicSystem;
import application.ValerianEconomicSystemBootstrap;
import application.inspection.*;
import behavior.health.profile.EconomicHealthAssessmentId;
import behavior.recommendation.profile.*;
import behavior.recommendation.repository.InMemoryInspectionRecommendationRepository;
import behavior.temporal.*;
import inspection.casefile.*;
import inspection.repository.InMemoryInspectionCaseRepository;
import inspection.investigation.casefile.*;
import inspection.investigation.repository.*;
import java.time.*;
import java.util.*;

public final class InspectionInvestigationServiceTest {
    public static void main(String[] args) {
        SeasonPeriod season = new SeasonPeriod(
                Season.SPRING,
                2026,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 5, 31));

        InMemoryInspectionRecommendationRepository recommendations =
                new InMemoryInspectionRecommendationRepository();
        InMemoryInspectionCaseRepository cases = new InMemoryInspectionCaseRepository();
        InMemoryEvidenceRecordRepository evidence = new InMemoryEvidenceRecordRepository();
        InMemoryHypothesisRecordRepository hypotheses = new InMemoryHypothesisRecordRepository();
        Instant now = Instant.parse("2026-04-15T12:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        InspectionRecommendation firstRecommendation = recommendation("health:test:m4.2:a", season);
        InspectionRecommendation secondRecommendation = recommendation("health:test:m4.2:b", season);
        recommendations.replaceAll(List.of(firstRecommendation, secondRecommendation));

        InspectionCaseService caseService = new InspectionCaseService(recommendations, cases, clock);
        InspectionCase firstCase = caseService.open(firstRecommendation);
        InspectionCase secondCase = caseService.open(secondRecommendation);
        InspectionInvestigationService service = new InspectionInvestigationService(
                cases, evidence, hypotheses, clock);

        EvidenceRecord transactionEvidence = service.registerEvidence(
                firstCase.id(),
                EvidenceRecordType.TRANSACTIONAL,
                "Repeated transfers",
                "Twenty transfers repeat the same amount and destination pattern.",
                "ledger://spring-2026/transfer-pattern-20",
                Instant.parse("2026-04-14T16:00:00Z"));
        EvidenceRecord testimony = service.registerEvidence(
                firstCase.id(),
                EvidenceRecordType.TESTIMONIAL,
                "Account holder statement",
                "The account holder states that the transfers correspond to independent purchases.",
                "statement://case-a/holder-1",
                Instant.parse("2026-04-15T09:00:00Z"));

        require(service.evidenceFor(firstCase.id()).size() == 2, "case must retain both evidence records");
        require(service.evidenceFor(secondCase.id()).isEmpty(), "evidence must remain isolated by case");
        require(transactionEvidence.registeredAt().equals(now), "registration instant mismatch");

        HypothesisRecord hypothesis = service.registerHypothesis(
                firstCase.id(),
                "The repeated transfers conceal coordinated value movement.");
        hypothesis = service.relateEvidence(
                hypothesis.id(), transactionEvidence.id(), HypothesisEvidenceImpact.SUPPORTS);
        hypothesis = service.relateEvidence(
                hypothesis.id(), testimony.id(), HypothesisEvidenceImpact.REFUTES);

        require(hypothesis.supportingEvidence().equals(Set.of(transactionEvidence.id())),
                "supporting evidence mismatch");
        require(hypothesis.refutingEvidence().equals(Set.of(testimony.id())),
                "refuting evidence mismatch");
        require(hypothesis.status() == HypothesisRecordStatus.OPEN,
                "contrasting evidence must not automatically conclude the hypothesis");

        hypothesis = service.concludeHypothesis(hypothesis.id(), HypothesisRecordStatus.INCONCLUSIVE);
        require(hypothesis.status() == HypothesisRecordStatus.INCONCLUSIVE,
                "inspector conclusion must be persisted");
        require(service.hypothesesFor(firstCase.id()).size() == 1,
                "case must expose its hypothesis records");

        EvidenceRecord foreignEvidence = service.registerEvidence(
                secondCase.id(), EvidenceRecordType.DOCUMENTARY,
                "Foreign case document", "Document belonging to another inspection case.",
                "document://case-b/1", Instant.parse("2026-04-15T10:00:00Z"));
        boolean crossCaseRejected = false;
        try {
            service.relateEvidence(hypothesis.id(), foreignEvidence.id(), HypothesisEvidenceImpact.SUPPORTS);
        } catch (IllegalArgumentException expected) {
            crossCaseRejected = true;
        }
        require(crossCaseRejected, "cross-case evidence relation must be rejected");

        boolean futureEvidenceRejected = false;
        try {
            service.registerEvidence(
                    firstCase.id(), EvidenceRecordType.OTHER,
                    "Future evidence", "Invalid temporal record.", "future://1",
                    now.plusSeconds(1));
        } catch (IllegalArgumentException expected) {
            futureEvidenceRejected = true;
        }
        require(futureEvidenceRejected, "future evidence acquisition must be rejected");

        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();
        require(system.getEvidenceRecordRepository() != null, "evidence repository not wired");
        require(system.getHypothesisRecordRepository() != null, "hypothesis repository not wired");
        require(system.getInspectionInvestigationService() != null, "investigation service not wired");

        System.out.println("InspectionInvestigationServiceTest: PASSED");
    }

    private static InspectionRecommendation recommendation(String assessmentValue, SeasonPeriod season) {
        EconomicHealthAssessmentId assessmentId = new EconomicHealthAssessmentId(assessmentValue);
        return new InspectionRecommendation(
                InspectionRecommendationId.from(assessmentId),
                assessmentId,
                season,
                InspectionRecommendationType.GROUP,
                Set.of(InspectionRecommendationReason.MIRROR_MOVEMENT),
                new InspectionRecommendationExplanation(
                        "Group inspection is institutionally justified.",
                        Set.of(), Set.of(), Set.of(), Set.of("Comerciante"), Set.of("Mercenario")));
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
