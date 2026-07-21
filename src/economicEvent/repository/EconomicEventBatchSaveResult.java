package economicEvent.repository;

/** Summary of one deterministic batch save. */
public record EconomicEventBatchSaveResult(int inspected, int created, int alreadyPresent) {
    public EconomicEventBatchSaveResult {
        if (inspected < 0 || created < 0 || alreadyPresent < 0) {
            throw new IllegalArgumentException("batch save counters must not be negative");
        }
        if (created + alreadyPresent != inspected) {
            throw new IllegalArgumentException("created plus alreadyPresent must equal inspected");
        }
    }
}
