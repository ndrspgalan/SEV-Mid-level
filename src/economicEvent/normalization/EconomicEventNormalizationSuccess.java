package economicEvent.normalization;

import economicEvent.EconomicEvent;

import java.util.List;
import java.util.Objects;

public record EconomicEventNormalizationSuccess(List<EconomicEvent> events)
        implements EconomicEventNormalizationResult {
    public EconomicEventNormalizationSuccess {
        Objects.requireNonNull(events, "events must not be null");
        events = List.copyOf(events);
        if (events.isEmpty()) throw new IllegalArgumentException("normalization success must contain at least one event");
    }
    public EconomicEventNormalizationSuccess(EconomicEvent event) { this(List.of(Objects.requireNonNull(event))); }
    @Override public boolean successful() { return true; }
}
