package behavior.profile;

import banking.identity.ConsumerId;
import java.util.Objects;

/** Stable identity of the behavioral projection for one consumer. */
public record BehaviorProfileId(ConsumerId consumerId) {
    public BehaviorProfileId {
        Objects.requireNonNull(consumerId, "consumer id must not be null");
    }
}
