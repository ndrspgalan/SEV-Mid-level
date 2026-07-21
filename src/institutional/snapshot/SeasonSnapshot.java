package institutional.snapshot;

import banking.identity.ProfessionCode;
import behavior.temporal.SeasonPeriod;

import java.util.*;

/** Complete institutional state observed during one Valerian season. */
public record SeasonSnapshot(SeasonPeriod seasonPeriod, Map<ProfessionCode, PopulationSnapshot> populations) {
    public SeasonSnapshot {
        Objects.requireNonNull(seasonPeriod);
        LinkedHashMap<ProfessionCode, PopulationSnapshot> copy = new LinkedHashMap<>();
        Objects.requireNonNull(populations).entrySet().stream()
                .sorted(Comparator.comparing(e -> e.getKey().value()))
                .forEach(e -> copy.put(Objects.requireNonNull(e.getKey()), Objects.requireNonNull(e.getValue())));
        populations = Collections.unmodifiableMap(copy);
    }
    public int totalPopulation() {
        return populations.values().stream().flatMap(p -> p.registeredConsumers().stream()).collect(java.util.stream.Collectors.toSet()).size();
    }
}
