package application;

import behavior.evidence.analysis.BehaviorEvidenceAnalyzer;
import behavior.evidence.repository.BehaviorEvidenceSetRepository;
import behavior.evidence.repository.InMemoryBehaviorEvidenceSetRepository;
import application.evidence.BehaviorEvidenceSetService;
import behavior.alignment.analysis.StructuralAlignmentAnalyzer;
import behavior.alignment.repository.StructuralAlignmentRepository;
import behavior.alignment.repository.InMemoryStructuralAlignmentRepository;
import application.alignment.StructuralAlignmentService;

import coinProperties.Currency;
import consumableRegistry.*;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;
import exchangeCoin.ExchangePolicy;
import exchangeCoin.ImplementedExchangePolicy;
import mintCoin.ImplementedMintPolicy;
import mintCoin.MintPolicy;
import mintCoin.MintSpecificationCatalog;
import operationalControl.OperationalControlService;
import operationalControl.OperationalDecisionJournal;
import operationalControl.OperationalPolicyRegistry;
import operationalControl.profile.ValerianProfessionCreditProfiles;
import operationalControl.profile.ValerianProfessionCreditProfileResolver;
import transaction.InMemoryTransactionLedger;
import transaction.TransactionLedger;
import transfer.ImplementedTransferPolicy;
import transfer.InMemoryTransferRequestRegistry;
import transfer.TransferPolicy;
import transfer.TransferRequestRegistry;
import application.analytics.projection.EconomicEventProjectionService;
import application.analytics.audit.EconomicEventInvariantAuditor;
import economicEvent.normalization.*;
import economicEvent.repository.EconomicEventRepository;
import economicEvent.repository.InMemoryEconomicEventRepository;
import behavior.aggregation.BehaviorAggregator;
import behavior.repository.BehaviorProfileRepository;
import behavior.repository.InMemoryBehaviorProfileRepository;
import application.behavior.BehaviorProfileService;
import application.behavior.ProfessionalBehaviorProfileService;
import behavior.temporal.ValerianSeasonResolver;
import behavior.temporal.analysis.ProfessionalBehaviorAnalyzer;
import behavior.temporal.repository.ProfessionalBehaviorProfileRepository;
import behavior.temporal.repository.InMemoryProfessionalBehaviorProfileRepository;
import application.institutional.InstitutionalSnapshotService;
import institutional.analysis.*;
import institutional.repository.*;
import behavior.expected.analysis.ExpectedBehaviorAnalyzer;
import behavior.expected.repository.*;
import application.expected.ExpectedBehaviorSetService;
import behavior.deviation.analysis.BehaviorDeviationAnalyzer;
import behavior.deviation.repository.*;
import application.deviation.BehaviorDeviationProfileService;
import behavior.alignment.analysis.StructuralAlignmentAnalyzer;
import behavior.alignment.repository.*;
import application.alignment.StructuralAlignmentService;
import behavior.evidence.analysis.BehaviorEvidenceAnalyzer;
import behavior.evidence.repository.*;
import application.evidence.BehaviorEvidenceSetService;
import behavior.correlation.analysis.EconomicCorrelationAnalyzer;
import behavior.correlation.repository.*;
import application.correlation.EconomicCorrelationGraphService;
import behavior.health.analysis.EconomicHealthAssessmentAnalyzer;
import behavior.health.repository.*;
import application.health.EconomicHealthAssessmentService;
import behavior.recommendation.analysis.InspectionRecommendationAnalyzer;
import behavior.recommendation.repository.*;
import application.recommendation.InspectionRecommendationService;
import inspection.repository.*;
import application.inspection.InspectionCaseService;
import inspection.investigation.repository.*;
import application.inspection.InspectionInvestigationService;
import inspection.resolution.repository.*;
import application.inspection.InspectionResolutionService;
import inspection.jurisprudence.repository.*;
import application.inspection.JurisprudentialComparisonService;
import inspection.doctrine.repository.*;
import application.inspection.SeasonalDoctrineUnificationService;
import application.inspection.DoctrineEvolutionService;
import inspection.doctrine.refund.repository.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class ValerianEconomicSystemBootstrap {

    private ValerianEconomicSystemBootstrap() {
    }

    public static ValerianEconomicSystem createJuniorSystem() {
        ConsumerRegistry consumerRegistry = new ConsumerRegistry();
        ConsumableRegistry consumableRegistry = new ConsumableRegistry();

        MintSpecificationCatalog mintSpecificationCatalog =
                MintSpecificationCatalog.valerianStandard();

        MintPolicy mintPolicy =
                new ImplementedMintPolicy(mintSpecificationCatalog);

        ExchangePolicy exchangePolicy =
                new ImplementedExchangePolicy();

        CommercialTransactionPolicy commercialTransactionPolicy =
                new ImplementedCommercialTransactionPolicy();

        TransactionLedger transactionLedger =
                new InMemoryTransactionLedger();

        TransferPolicy transferPolicy = new ImplementedTransferPolicy();
        TransferRequestRegistry transferRequestRegistry =
                new InMemoryTransferRequestRegistry();
        OperationalPolicyRegistry operationalPolicyRegistry = new OperationalPolicyRegistry();
        OperationalControlService operationalControlService = new OperationalControlService(operationalPolicyRegistry, ZoneOffset.UTC);
        OperationalDecisionJournal operationalDecisionJournal = new OperationalDecisionJournal();
        ValerianProfessionCreditProfiles.install(operationalPolicyRegistry, Instant.MIN);

        loadInitialConsumers(consumerRegistry);
        loadInitialConsumables(consumableRegistry);

        EconomicEventRepository economicEventRepository = new InMemoryEconomicEventRepository();
        CompositeEconomicEventNormalizer economicEventNormalizer = new CompositeEconomicEventNormalizer(
                new TransactionEconomicEventNormalizer(consumerRegistry),
                new AccountHistoryEconomicEventNormalizer(),
                new OperationalDecisionEconomicEventNormalizer());
        EconomicEventProjectionService economicEventProjectionService = new EconomicEventProjectionService(
                transactionLedger, consumerRegistry.getAccountHistoryJournal(), operationalDecisionJournal,
                economicEventRepository, economicEventNormalizer);
        EconomicEventInvariantAuditor economicEventInvariantAuditor = new EconomicEventInvariantAuditor(
                transactionLedger, consumerRegistry.getAccountHistoryJournal(), operationalDecisionJournal,
                economicEventRepository, economicEventNormalizer, Clock.systemUTC());
        BehaviorProfileRepository behaviorProfileRepository = new InMemoryBehaviorProfileRepository();
        BehaviorProfileService behaviorProfileService = new BehaviorProfileService(
                economicEventRepository, behaviorProfileRepository, new BehaviorAggregator());
        ProfessionalBehaviorProfileRepository professionalBehaviorProfileRepository =
                new InMemoryProfessionalBehaviorProfileRepository();
        ValerianProfessionCreditProfileResolver creditProfileResolver =
                new ValerianProfessionCreditProfileResolver();
        ValerianSeasonResolver seasonResolver = new ValerianSeasonResolver(ZoneOffset.UTC);
        ProfessionalBehaviorProfileService professionalBehaviorProfileService =
                new ProfessionalBehaviorProfileService(
                        economicEventRepository,
                        professionalBehaviorProfileRepository,
                        new ProfessionalBehaviorAnalyzer(
                                creditProfileResolver,
                                seasonResolver,
                                ZoneOffset.UTC));
        SeasonSnapshotRepository seasonSnapshotRepository = new InMemorySeasonSnapshotRepository();
        InstitutionalSnapshotService institutionalSnapshotService = new InstitutionalSnapshotService(
                consumerRegistry.getAccountHistoryJournal(), economicEventRepository, professionalBehaviorProfileRepository,
                seasonSnapshotRepository,
                new InstitutionalSnapshotAnalyzer(seasonResolver, creditProfileResolver, new CreditPrivilegeComparator(), ZoneOffset.UTC),
                new EconomicHealthAnalyzer());
        ExpectedBehaviorSetRepository expectedBehaviorSetRepository = new InMemoryExpectedBehaviorSetRepository();
        ExpectedBehaviorSetService expectedBehaviorSetService = new ExpectedBehaviorSetService(
                seasonSnapshotRepository, professionalBehaviorProfileRepository, economicEventRepository,
                expectedBehaviorSetRepository, new ExpectedBehaviorAnalyzer(seasonResolver));
        BehaviorDeviationProfileRepository behaviorDeviationProfileRepository = new InMemoryBehaviorDeviationProfileRepository();
        BehaviorDeviationProfileService behaviorDeviationProfileService = new BehaviorDeviationProfileService(
                seasonSnapshotRepository, professionalBehaviorProfileRepository, expectedBehaviorSetRepository,
                economicEventRepository, behaviorDeviationProfileRepository, new BehaviorDeviationAnalyzer(seasonResolver));
        BehaviorEvidenceSetRepository behaviorEvidenceSetRepository = new InMemoryBehaviorEvidenceSetRepository();
        BehaviorEvidenceSetService behaviorEvidenceSetService = new BehaviorEvidenceSetService(
                behaviorDeviationProfileRepository, seasonSnapshotRepository, behaviorEvidenceSetRepository,
                new BehaviorEvidenceAnalyzer());
        StructuralAlignmentRepository structuralAlignmentRepository = new InMemoryStructuralAlignmentRepository();
        StructuralAlignmentService structuralAlignmentService = new StructuralAlignmentService(
                behaviorEvidenceSetRepository, structuralAlignmentRepository, new StructuralAlignmentAnalyzer());
        EconomicCorrelationGraphRepository economicCorrelationGraphRepository =
                new InMemoryEconomicCorrelationGraphRepository();
        EconomicCorrelationGraphService economicCorrelationGraphService =
                new EconomicCorrelationGraphService(
                        structuralAlignmentRepository,
                        economicCorrelationGraphRepository,
                        new EconomicCorrelationAnalyzer());
        EconomicHealthAssessmentRepository economicHealthAssessmentRepository =
                new InMemoryEconomicHealthAssessmentRepository();
        EconomicHealthAssessmentService economicHealthAssessmentService =
                new EconomicHealthAssessmentService(
                        economicCorrelationGraphRepository,
                        economicHealthAssessmentRepository,
                        new EconomicHealthAssessmentAnalyzer());
        InspectionRecommendationRepository inspectionRecommendationRepository =
                new InMemoryInspectionRecommendationRepository();
        InspectionRecommendationService inspectionRecommendationService =
                new InspectionRecommendationService(
                        economicHealthAssessmentRepository,
                        inspectionRecommendationRepository,
                        new InspectionRecommendationAnalyzer());
        InspectionCaseRepository inspectionCaseRepository =
                new InMemoryInspectionCaseRepository();
        InspectionCaseService inspectionCaseService =
                new InspectionCaseService(
                        inspectionRecommendationRepository,
                        inspectionCaseRepository,
                        Clock.systemUTC());
        EvidenceRecordRepository evidenceRecordRepository =
                new InMemoryEvidenceRecordRepository();
        HypothesisRecordRepository hypothesisRecordRepository =
                new InMemoryHypothesisRecordRepository();
        InspectionInvestigationService inspectionInvestigationService =
                new InspectionInvestigationService(
                        inspectionCaseRepository,
                        evidenceRecordRepository,
                        hypothesisRecordRepository,
                        Clock.systemUTC());
        InspectionResolutionRepository inspectionResolutionRepository =
                new InMemoryInspectionResolutionRepository();
        JurisprudentialComparisonRepository jurisprudentialComparisonRepository =
                new InMemoryJurisprudentialComparisonRepository();
        JurisprudentialComparisonService jurisprudentialComparisonService =
                new JurisprudentialComparisonService(
                        inspectionCaseRepository,
                        inspectionResolutionRepository,
                        jurisprudentialComparisonRepository,
                        Clock.systemUTC());
        InspectionResolutionService inspectionResolutionService =
                new InspectionResolutionService(
                        inspectionCaseRepository,
                        evidenceRecordRepository,
                        hypothesisRecordRepository,
                        inspectionResolutionRepository,
                        jurisprudentialComparisonService,
                        Clock.systemUTC());
        UnifiedDoctrineCaseRepository unifiedDoctrineCaseRepository =
                new InMemoryUnifiedDoctrineCaseRepository();
        SeasonalDoctrineUnificationService seasonalDoctrineUnificationService =
                new SeasonalDoctrineUnificationService(
                        inspectionCaseRepository,
                        inspectionResolutionRepository,
                        jurisprudentialComparisonRepository,
                        unifiedDoctrineCaseRepository,
                        Clock.systemUTC());
        RefundedDoctrineCaseRepository refundedDoctrineCaseRepository =
                new InMemoryRefundedDoctrineCaseRepository();
        DoctrineEvolutionService doctrineEvolutionService =
                new DoctrineEvolutionService(
                        unifiedDoctrineCaseRepository,
                        refundedDoctrineCaseRepository,
                        Clock.systemUTC());

        return new ValerianEconomicSystem(
                consumerRegistry,
                consumableRegistry,
                mintPolicy,
                exchangePolicy,
                commercialTransactionPolicy,
                transactionLedger,
                transferPolicy,
                transferRequestRegistry,
                operationalPolicyRegistry,
                operationalControlService,
                operationalDecisionJournal,
                economicEventRepository,
                economicEventProjectionService,
                economicEventInvariantAuditor,
                behaviorProfileRepository,
                behaviorProfileService,
                professionalBehaviorProfileRepository,
                professionalBehaviorProfileService,
                seasonSnapshotRepository,
                institutionalSnapshotService,
                expectedBehaviorSetRepository,
                expectedBehaviorSetService,
                behaviorDeviationProfileRepository,
                behaviorDeviationProfileService,
                behaviorEvidenceSetRepository,
                behaviorEvidenceSetService,
                structuralAlignmentRepository,
                structuralAlignmentService,
                economicCorrelationGraphRepository,
                economicCorrelationGraphService,
                economicHealthAssessmentRepository,
                economicHealthAssessmentService,
                inspectionRecommendationRepository,
                inspectionRecommendationService,
                inspectionCaseRepository,
                inspectionCaseService,
                evidenceRecordRepository,
                hypothesisRecordRepository,
                inspectionInvestigationService,
                inspectionResolutionRepository,
                inspectionResolutionService,
                jurisprudentialComparisonRepository,
                jurisprudentialComparisonService,
                unifiedDoctrineCaseRepository,
                seasonalDoctrineUnificationService,
                refundedDoctrineCaseRepository,
                doctrineEvolutionService
        );
    }

    private static void loadInitialConsumers(ConsumerRegistry consumerRegistry) {
        Consumer buyer = consumerRegistry.registerExact(
                "Kenan",
                "Guerrero de Ébano",
                25399,
                0
        );

        consumerRegistry.register(
                "Daniel Strauss",
                "Comerciante"
        );

        buyer.getBankAccount().deposit(Currency.SUELDO, 500);
        buyer.getBankAccount().deposit(Currency.VALERITA, 10_000);
    }

    private static void loadInitialConsumables(
            ConsumableRegistry consumableRegistry
    ) {
        ValerianBasicConsumableCatalog.registerInto(consumableRegistry);
    }
}
