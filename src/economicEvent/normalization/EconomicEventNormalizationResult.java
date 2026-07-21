package economicEvent.normalization;

public sealed interface EconomicEventNormalizationResult
        permits EconomicEventNormalizationSuccess, EconomicEventNormalizationFailure {
    boolean successful();
}
