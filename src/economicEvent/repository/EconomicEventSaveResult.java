package economicEvent.repository;

/** Outcome of an idempotent repository save. */
public enum EconomicEventSaveResult {
    CREATED,
    ALREADY_PRESENT
}
