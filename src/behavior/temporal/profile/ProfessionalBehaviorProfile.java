package behavior.temporal.profile;

import banking.identity.ConsumerId;
import banking.identity.Profession;
import behavior.temporal.SeasonPeriod;
import consumableRegistry.ConsumableCategory;
import economicEvent.EconomicEventType;
import operationalControl.profile.ProfessionCreditProfile;

import java.time.Instant;
import java.util.*;

/**
 * Individual operational profile contextualized by frozen profession and season.
 *
 * <p>This profile is descriptive. It does not replace the static Junior credit
 * profile, does not construct the collective expected behavior and does not
 * classify risk or fraud. The global longitudinal {@code BehaviorProfile}
 * remains available for deep manual Inspection across the holder's complete
 * history.</p>
 */
public record ProfessionalBehaviorProfile(
        ProfessionalBehaviorProfileId id,
        ConsumerId consumerId,
        Profession profession,
        ProfessionCreditProfile creditProfile,
        SeasonPeriod seasonPeriod,
        Instant firstEventAt,
        Instant lastEventAt,
        long analyzedEvents,
        SeasonActivitySummary seasonActivity,
        Map<EconomicEventType, TemporalBehaviorStatistics> eventTypes,
        Map<EconomicEventType, SeasonActivitySummary> eventTypeSeasonActivity,
        Map<String, PurchaseBehaviorStatistics> consumables,
        Map<ConsumableCategory, PurchaseBehaviorStatistics> categories
) {
    public ProfessionalBehaviorProfile {
        Objects.requireNonNull(id); Objects.requireNonNull(consumerId); Objects.requireNonNull(profession);
        Objects.requireNonNull(creditProfile); Objects.requireNonNull(seasonPeriod); Objects.requireNonNull(firstEventAt); Objects.requireNonNull(lastEventAt);
        Objects.requireNonNull(seasonActivity);
        if (analyzedEvents <= 0 || lastEventAt.isBefore(firstEventAt)) throw new IllegalArgumentException("invalid professional behavior profile");
        if (seasonActivity.total() != analyzedEvents) throw new IllegalArgumentException("season summary does not match analyzed events");
        if (!id.consumerId().equals(consumerId) || !id.professionCode().equals(profession.code()) || !id.seasonPeriod().equals(seasonPeriod.label())) throw new IllegalArgumentException("profile identity mismatch");
        eventTypes = immutable(eventTypes);
        eventTypeSeasonActivity = immutable(eventTypeSeasonActivity);
        consumables = immutable(consumables);
        categories = immutable(categories);
    }
    private static <K,V> Map<K,V> immutable(Map<K,V> source) {
        Objects.requireNonNull(source); LinkedHashMap<K,V> copy = new LinkedHashMap<>();
        source.forEach((k,v) -> copy.put(Objects.requireNonNull(k), Objects.requireNonNull(v)));
        return Collections.unmodifiableMap(copy);
    }
}
