package economicEvent;

import banking.identity.BankAccountId;
import banking.identity.ConsumerId;
import banking.identity.InstitutionalAccountId;

import java.util.Objects;
import java.util.Optional;

/** Optional second participant of a bilateral economic event. */
public record EconomicCounterparty(
        BankAccountId accountId,
        Optional<ConsumerId> consumerId,
        Optional<InstitutionalAccountId> institutionalAccountId
) {
    public EconomicCounterparty {
        Objects.requireNonNull(accountId, "counterparty account id must not be null");
        consumerId = Objects.requireNonNull(consumerId, "counterparty consumer id optional must not be null");
        institutionalAccountId = Objects.requireNonNull(
                institutionalAccountId, "counterparty institutional account id optional must not be null");
    }

    public EconomicCounterparty(BankAccountId accountId) {
        this(accountId, Optional.empty(), Optional.empty());
    }
}
