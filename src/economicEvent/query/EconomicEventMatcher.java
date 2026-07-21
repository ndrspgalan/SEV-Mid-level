package economicEvent.query;

import economicEvent.EconomicAmount;
import economicEvent.EconomicEvent;

import java.util.Objects;

/** Single source of truth for repository and application-level query semantics. */
public final class EconomicEventMatcher {
    private EconomicEventMatcher() {}

    public static boolean matches(EconomicEvent event, EconomicEventQuery query) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(query, "query must not be null");
        return query.actorAccountId().map(id -> event.actor().accountId().equals(id)).orElse(true)
                && query.counterpartyAccountId().map(id -> event.counterparty()
                        .map(counterparty -> counterparty.accountId().equals(id)).orElse(false)).orElse(true)
                && query.consumerId().map(id -> event.actor().consumerId().equals(id)
                        || event.counterparty().flatMap(value -> value.consumerId()).map(id::equals).orElse(false)).orElse(true)
                && query.actorProfession().map(value -> event.actorProfession().map(value::equals).orElse(false)).orElse(true)
                && query.type().map(value -> event.type() == value).orElse(true)
                && query.category().map(value -> event.category() == value).orElse(true)
                && query.status().map(value -> event.status() == value).orElse(true)
                && matchesMonetaryCriteria(event, query)
                && query.occurredFromInclusive().map(value -> !event.occurredAt().isBefore(value)).orElse(true)
                && query.occurredToExclusive().map(value -> event.occurredAt().isBefore(value)).orElse(true)
                && query.sourceType().map(value -> event.source().type() == value).orElse(true)
                && query.sourceId().map(value -> event.source().sourceId().equals(value)).orElse(true)
                && query.rejected().map(value -> event.rejected() == value).orElse(true);
    }

    private static boolean matchesMonetaryCriteria(EconomicEvent event, EconomicEventQuery query) {
        if (query.currency().isEmpty() && query.minimumAmountInclusive().isEmpty()
                && query.maximumAmountInclusive().isEmpty()) return true;
        return event.primaryAmount().map(amount -> matchesAmount(amount, query)).orElse(false)
                || event.secondaryAmount().map(amount -> matchesAmount(amount, query)).orElse(false);
    }

    private static boolean matchesAmount(EconomicAmount amount, EconomicEventQuery query) {
        return query.currency().map(value -> amount.currency() == value).orElse(true)
                && query.minimumAmountInclusive().map(value -> amount.amount() >= value).orElse(true)
                && query.maximumAmountInclusive().map(value -> amount.amount() <= value).orElse(true);
    }

}
