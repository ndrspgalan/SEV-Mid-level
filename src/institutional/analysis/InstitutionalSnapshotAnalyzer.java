package institutional.analysis;

import accountHistory.*;
import banking.identity.*;
import banking.lifecycle.AccountClosureReason;
import behavior.temporal.*;
import behavior.temporal.profile.ProfessionalBehaviorProfile;
import economicEvent.*;
import institutional.snapshot.*;
import operationalControl.profile.ProfessionCreditProfileResolver;

import java.time.*;
import java.util.*;

/**
 * Reconstructs season-bounded institutional photographs from immutable history.
 * The time boundary always governs membership: a profession population contains
 * every distinct holder who was registered in that profession at any instant of
 * that season. Results remain descriptive and are not risk or fraud decisions.
 */
public final class InstitutionalSnapshotAnalyzer {
    private final SeasonResolver seasonResolver;
    private final ProfessionCreditProfileResolver creditProfiles;
    private final CreditPrivilegeComparator privilegeComparator;
    private final ZoneId zone;

    public InstitutionalSnapshotAnalyzer(SeasonResolver seasonResolver,
                                         ProfessionCreditProfileResolver creditProfiles,
                                         CreditPrivilegeComparator privilegeComparator,
                                         ZoneId zone) {
        this.seasonResolver = Objects.requireNonNull(seasonResolver);
        this.creditProfiles = Objects.requireNonNull(creditProfiles);
        this.privilegeComparator = Objects.requireNonNull(privilegeComparator);
        this.zone = Objects.requireNonNull(zone);
    }

    public List<SeasonSnapshot> analyze(List<AccountHistoryEvent> history,
                                        List<EconomicEvent> economicEvents,
                                        List<ProfessionalBehaviorProfile> behaviorProfiles) {
        Objects.requireNonNull(history); Objects.requireNonNull(economicEvents); Objects.requireNonNull(behaviorProfiles);
        TreeMap<String, SeasonPeriod> periods = new TreeMap<>();
        history.forEach(e -> { SeasonPeriod p=seasonResolver.resolve(e.occurredAt()); periods.put(sortKey(p), p); });
        economicEvents.forEach(e -> { SeasonPeriod p=seasonResolver.resolve(e.occurredAt()); periods.put(sortKey(p), p); });
        behaviorProfiles.forEach(p -> periods.put(sortKey(p.seasonPeriod()), p.seasonPeriod()));
        List<AccountHistoryEvent> orderedHistory = history.stream().filter(e -> e.status()==AccountHistoryEventStatus.COMPLETED)
                .sorted(Comparator.comparing(AccountHistoryEvent::occurredAt).thenComparing(e->e.eventId().value())).toList();
        List<EconomicEvent> orderedEconomic = economicEvents.stream().sorted(Comparator.comparing(EconomicEvent::occurredAt).thenComparing(e->e.id().value())).toList();
        List<SeasonSnapshot> result = new ArrayList<>();
        for (SeasonPeriod period : periods.values()) result.add(build(period, orderedHistory, orderedEconomic));
        return List.copyOf(result);
    }

    private SeasonSnapshot build(SeasonPeriod period, List<AccountHistoryEvent> history, List<EconomicEvent> economics) {
        Instant start = period.startsOn().atStartOfDay(zone).toInstant();
        Instant endExclusive = period.endsOn().plusDays(1).atStartOfDay(zone).toInstant();
        Map<BankAccountId, State> state = new HashMap<>();
        for (AccountHistoryEvent e : history) if (e.occurredAt().isBefore(start)) applyState(state, e);
        Map<ProfessionCode, Builder> builders = new LinkedHashMap<>();
        state.values().stream().filter(State::active).forEach(s -> builder(builders, s.profession).members.add(s.consumerId));

        for (AccountHistoryEvent e : history) {
            if (e.occurredAt().isBefore(start) || !e.occurredAt().isBefore(endExclusive)) continue;
            State before = state.get(e.bankAccountId());
            switch (e.type()) {
                case ACCOUNT_REGISTERED -> {
                    Profession p=e.currentProfession().orElseThrow(); Builder b=builder(builders,p);
                    b.members.add(e.consumerId()); b.registrations++;
                }
                case PROFESSION_CHANGED -> {
                    Profession from=e.previousProfession().orElseThrow(); Profession to=e.currentProfession().orElseThrow();
                    Builder out=builder(builders,from), in=builder(builders,to);
                    out.professionChangesOut++; in.professionChangesIn++; in.members.add(e.consumerId());
                    MobilityDirection d=privilegeComparator.compare(creditProfiles.resolve(from), creditProfiles.resolve(to));
                    if (d==MobilityDirection.UPWARD) { out.upwardMobilityOut++; in.upwardMobilityIn++; }
                    else if (d==MobilityDirection.DOWNWARD) { out.downwardMobilityOut++; in.downwardMobilityIn++; }
                }
                case HOLDER_RELEASED -> { Profession p=professionOf(e,before); Builder b=builder(builders,p); b.holderReleases++; }
                case HOLDER_ASSIGNED -> { Profession p=professionOf(e,before); Builder b=builder(builders,p); b.holderAssignments++; b.members.add(e.consumerId()); }
                case ACCOUNT_CLOSED -> { Profession p=professionOf(e,before); Builder b=builder(builders,p); b.accountClosures++; if (e.closureReason().orElse(null)==AccountClosureReason.HOLDER_DECEASED) b.deaths++; }
                default -> { }
            }
            applyState(state,e);
        }

        for (EconomicEvent e : economics) {
            if (e.type()!=EconomicEventType.FUNDS_TRANSFERRED || e.rejected() || e.occurredAt().isBefore(start) || !e.occurredAt().isBefore(endExclusive)) continue;
            e.actorProfession().ifPresent(p -> builder(builders,p).transfersSent++);
            e.counterparty().flatMap(EconomicCounterparty::consumerId).ifPresent(cid -> {
                Profession p=professionAtConsumer(history,cid,e.occurredAt()); if (p!=null) builder(builders,p).transfersReceived++;
            });
        }
        Map<ProfessionCode, PopulationSnapshot> populations=new LinkedHashMap<>();
        builders.values().stream().sorted(Comparator.comparing(b->b.profession.code().value())).forEach(b->populations.put(b.profession.code(),b.build(period)));
        return new SeasonSnapshot(period,populations);
    }

    private Profession professionAtConsumer(List<AccountHistoryEvent> history, ConsumerId id, Instant at) {
        Profession current=null; boolean active=false;
        for (AccountHistoryEvent e:history) {
            if (!e.consumerId().equals(id)||e.occurredAt().isAfter(at)||e.status()!=AccountHistoryEventStatus.COMPLETED) continue;
            switch(e.type()) {
                case ACCOUNT_REGISTERED, PROFESSION_CHANGED -> { current=e.currentProfession().orElse(current); active=true; }
                case HOLDER_RELEASED, ACCOUNT_CLOSED -> active=false;
                case HOLDER_ASSIGNED -> active=true;
                default -> { }
            }
        }
        return active?current:null;
    }

    private Profession professionOf(AccountHistoryEvent e, State state) {
        return e.currentProfession().or(() -> e.previousProfession()).orElseGet(() -> Objects.requireNonNull(state,"missing account state").profession);
    }
    private void applyState(Map<BankAccountId,State> states, AccountHistoryEvent e) {
        State old=states.get(e.bankAccountId());
        switch(e.type()) {
            case ACCOUNT_REGISTERED -> states.put(e.bankAccountId(),new State(e.consumerId(),e.currentProfession().orElseThrow(),true));
            case PROFESSION_CHANGED -> states.put(e.bankAccountId(),new State(e.consumerId(),e.currentProfession().orElseThrow(),old==null||old.active));
            case HOLDER_RELEASED, ACCOUNT_CLOSED -> { if(old!=null) states.put(e.bankAccountId(),new State(old.consumerId,old.profession,false)); }
            case HOLDER_ASSIGNED -> { Profession p=professionOf(e,old); states.put(e.bankAccountId(),new State(e.consumerId(),p,true)); }
            default -> { }
        }
    }
    private Builder builder(Map<ProfessionCode,Builder> map, Profession p){return map.computeIfAbsent(p.code(),k->new Builder(p));}
    private String sortKey(SeasonPeriod p){return p.startsOn()+"|"+p.label();}
    private record State(ConsumerId consumerId, Profession profession, boolean active){}
    private static final class Builder {
        final Profession profession; final Set<ConsumerId> members=new LinkedHashSet<>();
        long registrations,professionChangesIn,professionChangesOut,holderAssignments,holderReleases,deaths,accountClosures,transfersSent,transfersReceived,upwardMobilityIn,upwardMobilityOut,downwardMobilityIn,downwardMobilityOut;
        Builder(Profession p){profession=p;}
        PopulationSnapshot build(SeasonPeriod s){return new PopulationSnapshot(profession,s,members,registrations,professionChangesIn,professionChangesOut,holderAssignments,holderReleases,deaths,accountClosures,transfersSent,transfersReceived,upwardMobilityIn,upwardMobilityOut,downwardMobilityIn,downwardMobilityOut);}
    }
}
