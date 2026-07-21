package economicEvent;

import banking.identity.BankAccountId;
import banking.identity.ConsumerId;
import banking.identity.InstitutionalAccountId;

import java.util.Objects;
import java.util.Optional;

/** Primary account-holder perspective captured at the instant of the event. */
public record EconomicActor(
        BankAccountId accountId,
        ConsumerId consumerId,
        Optional<InstitutionalAccountId> institutionalAccountId
) {
    public EconomicActor {
        Objects.requireNonNull(accountId, "actor account id must not be null");
        Objects.requireNonNull(consumerId, "actor consumer id must not be null");
        institutionalAccountId = Objects.requireNonNull(
                institutionalAccountId, "actor institutional account id optional must not be null");
    }

    public EconomicActor(BankAccountId accountId, ConsumerId consumerId) {
        this(accountId, consumerId, Optional.empty());
    }
}
