package institutional.analysis;

import banking.identity.ProfessionCode;
import institutional.snapshot.*;
import java.util.*;

/** Produces season-over-season and five-season descriptive health views. */
public final class EconomicHealthAnalyzer {
    public EconomicHealthSnapshot analyze(List<SeasonSnapshot> snapshots) {
        if (snapshots==null || snapshots.isEmpty()) throw new IllegalArgumentException("at least one season snapshot is required");
        List<SeasonSnapshot> ordered=snapshots.stream().sorted(Comparator.comparing(s->s.seasonPeriod().startsOn())).toList();
        SeasonSnapshot current=ordered.get(ordered.size()-1);
        SeasonSnapshot previous=ordered.size()>1?ordered.get(ordered.size()-2):null;
        List<ProfessionEvolution> evolution=new ArrayList<>();
        if(previous!=null){
            Set<ProfessionCode> codes=new TreeSet<>(Comparator.comparing(ProfessionCode::value)); codes.addAll(previous.populations().keySet()); codes.addAll(current.populations().keySet());
            for(ProfessionCode code:codes){
                PopulationSnapshot p=previous.populations().get(code), c=current.populations().get(code);
                if(c==null&&p==null)continue;
                var profession=c!=null?c.profession():p.profession();
                int pp=p==null?0:p.populationSize(), cp=c==null?0:c.populationSize();
                long pt=p==null?0:p.transfersReceived()-p.transfersSent(), ct=c==null?0:c.transfersReceived()-c.transfersSent();
                long pu=p==null?0:p.netUpwardMobility(), cu=c==null?0:c.netUpwardMobility();
                long pd=p==null?0:p.netDownwardMobility(), cd=c==null?0:c.netDownwardMobility();
                long death=(c==null?0:c.deaths())-(p==null?0:p.deaths());
                long release=(c==null?0:c.holderReleases())-(p==null?0:p.holderReleases());
                evolution.add(new ProfessionEvolution(profession,previous.seasonPeriod(),current.seasonPeriod(),pp,cp,cp-pp,ct-pt,cu-pu,cd-pd,death,release));
            }
        }
        int from=Math.max(0,ordered.size()-5);
        return new EconomicHealthSnapshot(current.seasonPeriod(),Optional.ofNullable(previous).map(SeasonSnapshot::seasonPeriod),evolution,ordered.subList(from,ordered.size()));
    }
}
