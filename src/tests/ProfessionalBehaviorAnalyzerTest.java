package tests;

import banking.identity.*;
import behavior.temporal.*;
import behavior.temporal.analysis.*;
import behavior.temporal.profile.*;
import coinProperties.Currency;
import economicEvent.*;
import operationalControl.profile.ValerianProfessionCreditProfileResolver;

import java.time.*;
import java.util.*;

public final class ProfessionalBehaviorAnalyzerTest {
    private ProfessionalBehaviorAnalyzerTest() {}
    public static void main(String[] args) {
        ConsumerId consumer = ConsumerId.random(); BankAccountId account = BankAccountId.random(); BankAccountId seller = BankAccountId.random();
        Profession jornalero = new Profession("Jornalero", ProfessionCode.of("Jorn"));
        Profession cantero = new Profession("Cantero", ProfessionCode.of("Cant"));
        List<EconomicEvent> events = List.of(
                purchase("A", "2025-11-03T01:00:00Z", consumer, account, seller, jornalero),
                purchase("B", "2025-11-03T10:00:00Z", consumer, account, seller, jornalero),
                purchase("C", "2026-02-10T18:00:00Z", consumer, account, seller, cantero));
        ProfessionalBehaviorAnalyzer analyzer = new ProfessionalBehaviorAnalyzer(
                new ValerianProfessionCreditProfileResolver(), new ValerianSeasonResolver(ZoneOffset.UTC), ZoneOffset.UTC);
        List<ProfessionalBehaviorProfile> profiles = analyzer.analyze(events);
        check(profiles.size()==2,"profession and season separation");
        ProfessionalBehaviorProfile winter = profiles.stream().filter(p->p.profession().name().equals("Jornalero")).findFirst().orElseThrow();
        check(winter.seasonPeriod().season()==Season.WINTER,"winter resolution");
        check(winter.seasonPeriod().label().equals("Q1-2026"),"winter anchor year");
        check(winter.creditProfile().profession().equals("Jornalero"),"credit-profile firewall");
        check(winter.seasonActivity().byDayPeriod().get(DayPeriod.NIGHT)==1,"night slot");
        check(winter.seasonActivity().byDayPeriod().get(DayPeriod.MORNING)==1,"morning slot");
        WindowStatistics day = winter.eventTypes().get(EconomicEventType.PURCHASE_EXECUTED).at(ObservationWindow.SAME_DAY);
        check(day.totalOccurrences()==2,"daily occurrences");
        check(day.maximum()==2,"same-day concentration");
        check(winter.seasonActivity().total()==2,"explicit seasonal summary");
        PurchaseBehaviorStatistics pan = winter.consumables().get("FOOD-001");
        check(pan.purchases().at(ObservationWindow.WEEK).totalOccurrences()==2,"purchase frequency");
        check(pan.units().at(ObservationWindow.WEEK).totalOccurrences()==2,"purchase units");
        check(pan.monetaryVolume().get(Currency.VALERITA).at(ObservationWindow.WEEK).totalOccurrences()==6,"currency-separated monetary volume");
        var report = analyzer.analyzeWithReport(events);
        check(report.examinedEvents()==3 && report.omittedWithoutHistoricalProfession()==0,"auditable analysis report");
        System.out.println("ProfessionalBehaviorAnalyzerTest: PASSED");
    }
    private static EconomicEvent purchase(String sourceId,String at,ConsumerId c,BankAccountId a,BankAccountId seller,Profession profession){
        EconomicEventSource source=new EconomicEventSource(EconomicEventSourceType.TRANSACTION_LEDGER,sourceId,"PurchaseTransactionDetails");
        return new EconomicEvent(source.eventId(),Instant.parse(at),EconomicEventType.PURCHASE_EXECUTED,EconomicEventCategory.COMMERCIAL,EconomicEventStatus.SUCCEEDED,
                new EconomicActor(a,c),Optional.of(new EconomicCounterparty(seller)),Optional.of(new EconomicAmount(Currency.VALERITA,3)),Optional.empty(),Optional.of(profession),Optional.of("FOOD"),Optional.empty(),source,
                Map.of("consumableId","FOOD-001","consumableName","Pan","consumableCategory","FOOD","quantity","1","unitPrice","3"));
    }
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
