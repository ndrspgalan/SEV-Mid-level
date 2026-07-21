package application.inspection;

import inspection.casefile.*;
import inspection.repository.InspectionCaseRepository;
import inspection.resolution.casefile.*;
import inspection.resolution.repository.InspectionResolutionRepository;
import inspection.jurisprudence.casefile.*;
import inspection.jurisprudence.repository.JurisprudentialComparisonRepository;
import java.time.Clock;
import java.util.*;

/**
 * M5.1 compares a candidate final resolution with prior closed, univocally similar
 * cases from the same concrete Season. It neither changes nor unifies doctrine.
 */
public final class JurisprudentialComparisonService {
    private final InspectionCaseRepository cases;
    private final InspectionResolutionRepository resolutions;
    private final JurisprudentialComparisonRepository comparisons;
    private final Clock clock;

    public JurisprudentialComparisonService(
            InspectionCaseRepository cases,
            InspectionResolutionRepository resolutions,
            JurisprudentialComparisonRepository comparisons,
            Clock clock
    ) {
        this.cases = Objects.requireNonNull(cases);
        this.resolutions = Objects.requireNonNull(resolutions);
        this.comparisons = Objects.requireNonNull(comparisons);
        this.clock = Objects.requireNonNull(clock);
    }

    public JurisprudentialComparison compareBeforeClosing(
            InspectionCase currentCase,
            InspectionResolutionType currentResolution
    ) {
        Objects.requireNonNull(currentCase); Objects.requireNonNull(currentResolution);
        if (!currentCase.isOpen()) throw new IllegalStateException("jurisprudential comparison requires an open case");
        if (comparisons.findByCaseId(currentCase.id()).isPresent()) {
            throw new IllegalStateException("inspection case already has a jurisprudential comparison");
        }

        JurisprudentialSimilarityKey key = JurisprudentialSimilarityKey.from(currentCase.sourceRecommendation());
        LinkedHashSet<InspectionCaseId> comparedCaseIds = new LinkedHashSet<>();
        EnumMap<InspectionResolutionType, Integer> frequencies = new EnumMap<>(InspectionResolutionType.class);

        for (InspectionCase precedent : cases.findAll()) {
            if (precedent.id().equals(currentCase.id()) || precedent.isOpen()) continue;
            if (!precedent.openedSeason().equals(currentCase.openedSeason())) continue;
            if (!JurisprudentialSimilarityKey.from(precedent.sourceRecommendation()).equals(key)) continue;

            InspectionResolution resolution = resolutions.findByCaseId(precedent.id())
                    .orElseThrow(() -> new IllegalStateException("closed precedent without final resolution: " + precedent.id()));
            comparedCaseIds.add(precedent.id());
            frequencies.merge(resolution.type(), 1, Integer::sum);
        }

        Optional<InspectionResolutionType> dominant = uniqueMode(frequencies);
        JurisprudentialAgreement agreement = dominant
                .map(value -> value == currentResolution ? JurisprudentialAgreement.YES : JurisprudentialAgreement.NO)
                .orElse(JurisprudentialAgreement.NO_CONSTA);

        JurisprudentialComparison comparison = new JurisprudentialComparison(
                JurisprudentialComparisonId.from(currentCase.id()),
                currentCase.id(),
                currentCase.openedSeason(),
                key,
                comparedCaseIds,
                currentResolution,
                dominant,
                agreement,
                clock.instant());
        return comparisons.save(comparison);
    }

    public Optional<JurisprudentialComparison> findByCaseId(InspectionCaseId caseId) {
        return comparisons.findByCaseId(Objects.requireNonNull(caseId));
    }

    public List<JurisprudentialComparison> findAll() { return comparisons.findAll(); }
    public long count() { return comparisons.count(); }

    private static Optional<InspectionResolutionType> uniqueMode(
            EnumMap<InspectionResolutionType, Integer> frequencies
    ) {
        if (frequencies.isEmpty()) return Optional.empty();
        int maximum = frequencies.values().stream().mapToInt(Integer::intValue).max().orElseThrow();
        List<InspectionResolutionType> leaders = frequencies.entrySet().stream()
                .filter(entry -> entry.getValue() == maximum)
                .map(Map.Entry::getKey)
                .toList();
        return leaders.size() == 1 ? Optional.of(leaders.get(0)) : Optional.empty();
    }
}
