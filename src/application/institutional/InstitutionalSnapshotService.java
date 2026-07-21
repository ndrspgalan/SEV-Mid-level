package application.institutional;
import accountHistory.AccountHistoryJournal;
import behavior.temporal.repository.ProfessionalBehaviorProfileRepository;
import economicEvent.repository.EconomicEventRepository;
import institutional.analysis.*;
import institutional.repository.SeasonSnapshotRepository;
import institutional.snapshot.*;
import java.util.*;
public final class InstitutionalSnapshotService {
 private final AccountHistoryJournal history; private final EconomicEventRepository events; private final ProfessionalBehaviorProfileRepository profiles; private final SeasonSnapshotRepository snapshots; private final InstitutionalSnapshotAnalyzer analyzer; private final EconomicHealthAnalyzer healthAnalyzer;
 public InstitutionalSnapshotService(AccountHistoryJournal h,EconomicEventRepository e,ProfessionalBehaviorProfileRepository p,SeasonSnapshotRepository s,InstitutionalSnapshotAnalyzer a,EconomicHealthAnalyzer ha){history=Objects.requireNonNull(h);events=Objects.requireNonNull(e);profiles=Objects.requireNonNull(p);snapshots=Objects.requireNonNull(s);analyzer=Objects.requireNonNull(a);healthAnalyzer=Objects.requireNonNull(ha);}
 public List<SeasonSnapshot> rebuildSnapshots(){List<SeasonSnapshot> built=analyzer.analyze(history.findAll(),events.findAll(),profiles.findAll());snapshots.replaceAll(built);return built;}
 public List<SeasonSnapshot> findAll(){return snapshots.findAll();}
 public EconomicHealthSnapshot currentHealth(){List<SeasonSnapshot> all=snapshots.findAll();if(all.isEmpty())all=rebuildSnapshots();return healthAnalyzer.analyze(all);}
}
