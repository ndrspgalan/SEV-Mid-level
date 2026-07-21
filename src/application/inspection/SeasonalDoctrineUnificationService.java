package application.inspection;

import behavior.temporal.SeasonPeriod;
import inspection.casefile.*;
import inspection.repository.InspectionCaseRepository;
import inspection.doctrine.casefile.*;
import inspection.doctrine.repository.UnifiedDoctrineCaseRepository;
import inspection.jurisprudence.casefile.*;
import inspection.jurisprudence.repository.JurisprudentialComparisonRepository;
import inspection.resolution.casefile.*;
import inspection.resolution.repository.InspectionResolutionRepository;
import java.time.Clock;
import java.util.*;

/** M5.2 closes seasonal jurisprudence by creating one doctrine case per eligible financial family. */
public final class SeasonalDoctrineUnificationService {
    private final InspectionCaseRepository cases;
    private final InspectionResolutionRepository resolutions;
    private final JurisprudentialComparisonRepository comparisons;
    private final UnifiedDoctrineCaseRepository doctrines;
    private final Clock clock;

    public SeasonalDoctrineUnificationService(
            InspectionCaseRepository cases,
            InspectionResolutionRepository resolutions,
            JurisprudentialComparisonRepository comparisons,
            UnifiedDoctrineCaseRepository doctrines,
            Clock clock
    ) {
        this.cases = Objects.requireNonNull(cases); this.resolutions = Objects.requireNonNull(resolutions);
        this.comparisons = Objects.requireNonNull(comparisons); this.doctrines = Objects.requireNonNull(doctrines);
        this.clock = Objects.requireNonNull(clock);
    }

    public List<UnifiedDoctrineCase> unifySeason(SeasonPeriod season) {
        Objects.requireNonNull(season);
        List<InspectionCase> seasonalCases = cases.findAll().stream()
                .filter(value -> value.openedSeason().equals(season)).toList();
        if (seasonalCases.stream().anyMatch(InspectionCase::isOpen)) {
            throw new IllegalStateException("seasonal doctrine cannot be unified while inspection cases remain open");
        }

        Map<JurisprudentialSimilarityKey, List<InspectionCase>> families = new LinkedHashMap<>();
        for (InspectionCase inspectionCase : seasonalCases) {
            JurisprudentialSimilarityKey key = JurisprudentialSimilarityKey.from(inspectionCase.sourceRecommendation());
            families.computeIfAbsent(key, ignored -> new ArrayList<>()).add(inspectionCase);
        }

        List<UnifiedDoctrineCase> result = new ArrayList<>();
        for (Map.Entry<JurisprudentialSimilarityKey, List<InspectionCase>> entry : families.entrySet()) {
            if (entry.getValue().size() < 2) continue;
            result.add(unifyFamily(season, entry.getKey(), entry.getValue()));
        }
        return List.copyOf(result);
    }

    public Optional<UnifiedDoctrineCase> findBySeasonAndKey(SeasonPeriod season, JurisprudentialSimilarityKey key) {
        return doctrines.findBySeasonAndKey(Objects.requireNonNull(season), Objects.requireNonNull(key));
    }
    public List<UnifiedDoctrineCase> findBySeason(SeasonPeriod season) { return doctrines.findBySeason(season); }
    public List<UnifiedDoctrineCase> findAll() { return doctrines.findAll(); }
    public long count() { return doctrines.count(); }

    private UnifiedDoctrineCase unifyFamily(SeasonPeriod season, JurisprudentialSimilarityKey key, List<InspectionCase> family) {
        LinkedHashSet<InspectionCaseId> sourceIds = new LinkedHashSet<>();
        EnumMap<JurisprudentialAgreement, Integer> agreements = new EnumMap<>(JurisprudentialAgreement.class);
        EnumMap<InspectionResolutionType, Integer> resolutionFrequencies = new EnumMap<>(InspectionResolutionType.class);

        for (InspectionCase inspectionCase : family) {
            InspectionResolution resolution = resolutions.findByCaseId(inspectionCase.id())
                    .orElseThrow(() -> new IllegalStateException("closed case without final resolution: " + inspectionCase.id()));
            JurisprudentialComparison comparison = comparisons.findByCaseId(inspectionCase.id())
                    .orElseThrow(() -> new IllegalStateException("closed case without jurisprudential comparison: " + inspectionCase.id()));
            if (!comparison.seasonPeriod().equals(season) || !comparison.similarityKey().equals(key)) {
                throw new IllegalStateException("jurisprudential comparison does not match its seasonal doctrine family");
            }
            sourceIds.add(inspectionCase.id());
            agreements.merge(comparison.agreement(), 1, Integer::sum);
            resolutionFrequencies.merge(resolution.type(), 1, Integer::sum);
        }

        JurisprudentialConsensusType consensus = classify(agreements);
        DoctrineUnificationMethod method = switch (consensus) {
            case ABSOLUTE_AGREEMENT, RELATIVE_AGREEMENT -> DoctrineUnificationMethod.DOMINANT_AGREEMENT;
            case NULL_AGREEMENT, ABSOLUTE_DISAGREEMENT -> DoctrineUnificationMethod.FORCED_MODE;
        };
        InspectionResolutionType doctrineResolution = uniqueMode(resolutionFrequencies)
                .orElse(InspectionResolutionType.TO_FILE);

        UnifiedDoctrineCase doctrineCase = new UnifiedDoctrineCase(
                UnifiedDoctrineCaseId.from(season, key), season, key, doctrineResolution,
                consensus, method, sourceIds.size(), sourceIds, clock.instant());
        return doctrines.save(doctrineCase);
    }

    private static JurisprudentialConsensusType classify(EnumMap<JurisprudentialAgreement, Integer> frequencies) {
        int yes = frequencies.getOrDefault(JurisprudentialAgreement.YES, 0);
        int no = frequencies.getOrDefault(JurisprudentialAgreement.NO, 0);
        if (yes == 0 && no == 0) return JurisprudentialConsensusType.NULL_AGREEMENT;
        if (no == 0) return JurisprudentialConsensusType.ABSOLUTE_AGREEMENT;
        if (yes == 0 || yes == no) return JurisprudentialConsensusType.ABSOLUTE_DISAGREEMENT;
        return JurisprudentialConsensusType.RELATIVE_AGREEMENT;
    }

    private static Optional<InspectionResolutionType> uniqueMode(EnumMap<InspectionResolutionType, Integer> frequencies) {
        int maximum = frequencies.values().stream().mapToInt(Integer::intValue).max().orElseThrow();
        List<InspectionResolutionType> leaders = frequencies.entrySet().stream()
                .filter(entry -> entry.getValue() == maximum).map(Map.Entry::getKey).toList();
        return leaders.size() == 1 ? Optional.of(leaders.get(0)) : Optional.empty();
    }
}
