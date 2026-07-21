package behavior.temporal.profile;

import behavior.temporal.ObservationWindow;
import java.util.*;

/**
 * Descriptive activity statistics over fixed, homogeneous buckets.
 *
 * <p>Zero buckets are retained: inactivity is part of the observed behavior.
 * The object describes activity; it never labels that activity as normal,
 * risky or fraudulent.</p>
 */
public record TemporalBehaviorStatistics(Map<ObservationWindow, WindowStatistics> byWindow) {
    public TemporalBehaviorStatistics {
        Objects.requireNonNull(byWindow);
        EnumMap<ObservationWindow, WindowStatistics> copy = new EnumMap<>(ObservationWindow.class);
        byWindow.forEach((k,v) -> copy.put(Objects.requireNonNull(k), Objects.requireNonNull(v)));
        if (copy.size() != ObservationWindow.values().length) throw new IllegalArgumentException("all observation windows are required");
        byWindow = Collections.unmodifiableMap(copy);
    }
    public WindowStatistics at(ObservationWindow window) { return byWindow.get(Objects.requireNonNull(window)); }
}
