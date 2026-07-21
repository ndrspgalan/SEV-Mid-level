package economicEvent.normalization;

public enum EconomicEventNormalizationFailureReason {
    MISSING_ACTOR,
    MISSING_COUNTERPARTY,
    MISSING_AMOUNT,
    MISSING_CURRENCY,
    INCONSISTENT_SOURCE_DATA,
    UNSUPPORTED_SOURCE_TYPE
}
