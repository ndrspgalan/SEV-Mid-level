package tests;

import accountHistory.*;
import banking.census.ProfessionCatalog;
import banking.identity.*;
import behavior.temporal.ValerianSeasonResolver;
import institutional.analysis.*;
import institutional.snapshot.*;
import operationalControl.profile.ValerianProfessionCreditProfileResolver;

import java.time.*;
import java.util.*;

public final class InstitutionalSnapshotAnalyzerTest {
    public static void main(String[] args) {
        ProfessionCatalog catalog=ProfessionCatalog.valerianStandard();
        Profession mendigo=catalog.require("Mendigo"), jornalero=catalog.require("Jornalero");
        ConsumerId consumer=ConsumerId.random(); BankAccountId account=BankAccountId.random();
        List<AccountHistoryEvent> history=List.of(
                event(account,consumer,AccountHistoryEventType.ACCOUNT_REGISTERED,Instant.parse("2025-11-10T10:00:00Z"),null,mendigo),
                event(account,consumer,AccountHistoryEventType.PROFESSION_CHANGED,Instant.parse("2026-02-10T10:00:00Z"),mendigo,jornalero)
        );
        InstitutionalSnapshotAnalyzer analyzer=new InstitutionalSnapshotAnalyzer(
                new ValerianSeasonResolver(ZoneOffset.UTC),new ValerianProfessionCreditProfileResolver(),new CreditPrivilegeComparator(),ZoneOffset.UTC);
        List<SeasonSnapshot> snapshots=analyzer.analyze(history,List.of(),List.of());
        require(snapshots.size()==2,"two seasons expected");
        PopulationSnapshot winter=snapshots.get(0).populations().get(mendigo.code());
        PopulationSnapshot spring=snapshots.get(1).populations().get(jornalero.code());
        require(winter.populationSize()==1,"winter population");
        require(spring.populationSize()==1,"spring population");
        require(spring.professionChangesIn()==1,"change in");
        require(spring.upwardMobilityIn()==1,"mendigo to jornalero must be upward by hard thresholds");
        System.out.println("InstitutionalSnapshotAnalyzerTest: PASSED");
    }
    private static AccountHistoryEvent event(BankAccountId a,ConsumerId c,AccountHistoryEventType t,Instant at,Profession prev,Profession cur){
        return new AccountHistoryEvent(AccountHistoryEventId.generate(),a,c,t,AccountHistoryEventStatus.COMPLETED,at,prev,cur,null,null,null,null,null,"TEST");
    }
    private static void require(boolean c,String m){if(!c)throw new AssertionError(m);}
}
