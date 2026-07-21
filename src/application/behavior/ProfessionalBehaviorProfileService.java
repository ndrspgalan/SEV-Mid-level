package application.behavior;

import behavior.temporal.analysis.ProfessionalBehaviorAnalysisReport;
import behavior.temporal.analysis.ProfessionalBehaviorAnalyzer;
import behavior.temporal.profile.*;
import behavior.temporal.repository.*;
import economicEvent.repository.EconomicEventRepository;

import java.util.*;

/**
 * Rebuilds the descriptive professional projection without altering the global
 * longitudinal profile used by manual Inspection.
 */
public final class ProfessionalBehaviorProfileService {
    private final EconomicEventRepository events;
    private final ProfessionalBehaviorProfileRepository profiles;
    private final ProfessionalBehaviorAnalyzer analyzer;
    private ProfessionalBehaviorAnalysisReport lastReport = new ProfessionalBehaviorAnalysisReport(0, 0, List.of());

    public ProfessionalBehaviorProfileService(EconomicEventRepository events, ProfessionalBehaviorProfileRepository profiles, ProfessionalBehaviorAnalyzer analyzer) {
        this.events=Objects.requireNonNull(events);
        this.profiles=Objects.requireNonNull(profiles);
        this.analyzer=Objects.requireNonNull(analyzer);
    }

    public List<ProfessionalBehaviorProfile> rebuildProfiles() {
        lastReport = analyzer.analyzeWithReport(events.findAll());
        profiles.replaceAll(lastReport.profiles());
        return lastReport.profiles();
    }

    public ProfessionalBehaviorAnalysisReport rebuildProfilesWithReport() {
        rebuildProfiles();
        return lastReport;
    }

    public ProfessionalBehaviorAnalysisReport lastReport() { return lastReport; }
    public Optional<ProfessionalBehaviorProfile> findById(ProfessionalBehaviorProfileId id){return profiles.findById(id);}
    public List<ProfessionalBehaviorProfile> findAll(){return profiles.findAll();}
    public long count(){return profiles.count();}
}
