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
import inspection.resolution.casefile.*;
import inspection.resolution.repository.*;
import inspection.jurisprudence.repository.*;
import java.time.*;
import java.util.*;

public final class InspectionResolutionServiceTest {
    public static void main(String[] args) {
        SeasonPeriod season = new SeasonPeriod(
                Season.SPRING, 2026,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 5, 31));
        Instant now = Instant.parse("2026-05-20T12:00:00Z");
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);

        InMemoryInspectionRecommendationRepository recommendations = new InMemoryInspectionRecommendationRepository();
        InMemoryInspectionCaseRepository cases = new InMemoryInspectionCaseRepository();
        InMemoryEvidenceRecordRepository evidence = new InMemoryEvidenceRecordRepository();
        InMemoryHypothesisRecordRepository hypotheses = new InMemoryHypothesisRecordRepository();
        InMemoryInspectionResolutionRepository resolutions = new InMemoryInspectionResolutionRepository();

        InspectionRecommendation auditRecommendation = recommendation("health:test:m4.3:audit", season);
        InspectionRecommendation fraudRecommendation = recommendation("health:test:m4.3:fraud", season);
        InspectionRecommendation fileRecommendation = recommendation("health:test:m4.3:file", season);
        recommendations.replaceAll(List.of(auditRecommendation, fraudRecommendation, fileRecommendation));

        InspectionCaseService caseService = new InspectionCaseService(recommendations, cases, clock);
        InspectionCase auditCase = caseService.open(auditRecommendation);
        InspectionCase fraudCase = caseService.open(fraudRecommendation);
        InspectionCase fileCase = caseService.open(fileRecommendation);
        InspectionInvestigationService investigation = new InspectionInvestigationService(cases, evidence, hypotheses, clock);
        JurisprudentialComparisonService jurisprudence = new JurisprudentialComparisonService(
                cases, resolutions, new InMemoryJurisprudentialComparisonRepository(), clock);
        InspectionResolutionService service = new InspectionResolutionService(
                cases, evidence, hypotheses, resolutions, jurisprudence, clock);

        EvidenceRecord auditEvidence = investigation.registerEvidence(
                auditCase.id(), EvidenceRecordType.TRANSACTIONAL,
                "Basic consumable pressure", "Repeated purchases show an unjustified survival bottleneck.",
                "ledger://spring-2026/basic-consumables", now.minusSeconds(3600));
        HypothesisRecord auditHypothesis = investigation.registerHypothesis(
                auditCase.id(), "The deployed price configuration is too restrictive for this society.");
        auditHypothesis = investigation.relateEvidence(
                auditHypothesis.id(), auditEvidence.id(), HypothesisEvidenceImpact.SUPPORTS);
        auditHypothesis = investigation.concludeHypothesis(
                auditHypothesis.id(), HypothesisRecordStatus.SUPPORTED);

        InstitutionalActionPlan auditPlan = new InstitutionalActionPlan(
                List.of(
                        new PolicyAdjustment(
                                InstitutionalActionScope.CONSUMABLE,
                                PolicyAdjustmentDirection.RELAX,
                                "BASIC_CONSUMABLES",
                                "Reduce the deployed price pressure over essential consumables."),
                        new PolicyAdjustment(
                                InstitutionalActionScope.SYSTEM,
                                PolicyAdjustmentDirection.RECONFIGURE,
                                "VALERITA_SUELDO_EXCHANGE",
                                "Reconfigure the deployment exchange conditions and observe the next season.")),
                List.of());
        InspectionResolution audit = service.resolve(
                auditCase.id(), InspectionResolutionType.AUDIT, auditPlan,
                new InspectionResolutionExplanation(
                        "The anomaly is better explained by the deployed configuration than by actor misconduct.",
                        Set.of(auditEvidence.id()), Set.of(auditHypothesis.id())));

        require(audit.type() == InspectionResolutionType.AUDIT, "audit type mismatch");
        require(audit.actionPlan().policyAdjustments().size() == 2, "audit adjustments mismatch");
        require(!cases.findById(auditCase.id()).orElseThrow().isOpen(), "resolved audit case must close");
        require(audit.resolvedAt().equals(now), "resolution instant mismatch");

        EvidenceRecord fraudEvidence = investigation.registerEvidence(
                fraudCase.id(), EvidenceRecordType.BEHAVIORAL,
                "Coordinated minting pattern", "A professional group repeatedly exploits deployed minting permissions.",
                "behavior://spring-2026/coordinated-minting", now.minusSeconds(1800));
        InstitutionalActionPlan fraudPlan = new InstitutionalActionPlan(
                List.of(
                        new PolicyAdjustment(
                                InstitutionalActionScope.SYSTEM,
                                PolicyAdjustmentDirection.RESTRICT,
                                "REALES_DE_A5_MINT_POLICY",
                                "Tighten the deployed minting policy for REALES DE A5."),
                        new PolicyAdjustment(
                                InstitutionalActionScope.SYSTEM,
                                PolicyAdjustmentDirection.RELAX,
                                "VALERITA_SUELDO_EXCHANGE",
                                "Relax this exchange route to contain collateral economic damage.")),
                List.of(new RestrictiveMeasure(
                        InstitutionalActionScope.PROFESSION,
                        "COMERCIANTE",
                        "Temporarily suspend minting permissions for the responsible profession.")));
        InspectionResolution fraud = service.resolve(
                fraudCase.id(), InspectionResolutionType.FRAUD, fraudPlan,
                new InspectionResolutionExplanation(
                        "Current professional behavior cannot be justified without constraining its operating margin.",
                        Set.of(fraudEvidence.id()), Set.of()));

        require(fraud.actionPlan().restrictiveMeasures().size() == 1, "fraud restriction mismatch");
        require(fraud.actionPlan().policyAdjustments().get(1).direction() == PolicyAdjustmentDirection.RELAX,
                "FRAUD must allow simultaneous compensating relaxation");

        InspectionResolution filed = service.resolve(
                fileCase.id(), InspectionResolutionType.TO_FILE, InstitutionalActionPlan.empty(),
                new InspectionResolutionExplanation(
                        "The investigated anomaly does not require deployment intervention.", Set.of(), Set.of()));
        require(filed.actionPlan().isEmpty(), "TO_FILE must be actionless");

        boolean auditRestrictionRejected = false;
        try {
            new InspectionResolution(
                    InspectionResolutionId.from(auditCase.id()), auditCase.id(), season,
                    InspectionResolutionType.AUDIT,
                    new InstitutionalActionPlan(
                            List.of(new PolicyAdjustment(InstitutionalActionScope.ACCOUNT,
                                    PolicyAdjustmentDirection.RECONFIGURE, "A", "Reconfigure account policy.")),
                            List.of(new RestrictiveMeasure(InstitutionalActionScope.ACCOUNT, "A", "Pause account."))),
                    new InspectionResolutionExplanation("Invalid audit.", Set.of(), Set.of()), now);
        } catch (IllegalArgumentException expected) {
            auditRestrictionRejected = true;
        }
        require(auditRestrictionRejected, "AUDIT restrictive measure must be rejected");

        boolean secondResolutionRejected = false;
        try {
            service.resolve(auditCase.id(), InspectionResolutionType.TO_FILE, InstitutionalActionPlan.empty(),
                    new InspectionResolutionExplanation("Duplicate.", Set.of(), Set.of()));
        } catch (IllegalStateException expected) {
            secondResolutionRejected = true;
        }
        require(secondResolutionRejected, "closed case cannot be resolved twice");

        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();
        require(system.getInspectionResolutionRepository() != null, "resolution repository not wired");
        require(system.getInspectionResolutionService() != null, "resolution service not wired");

        System.out.println("InspectionResolutionServiceTest: PASSED");
    }

    private static InspectionRecommendation recommendation(String assessmentValue, SeasonPeriod season) {
        EconomicHealthAssessmentId assessmentId = new EconomicHealthAssessmentId(assessmentValue);
        return new InspectionRecommendation(
                InspectionRecommendationId.from(assessmentId), assessmentId, season,
                InspectionRecommendationType.GROUP,
                Set.of(InspectionRecommendationReason.MIRROR_MOVEMENT),
                new InspectionRecommendationExplanation(
                        "Inspection is institutionally justified.",
                        Set.of(), Set.of(), Set.of(), Set.of("Comerciante"), Set.of("Mercenario")));
    }

    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
