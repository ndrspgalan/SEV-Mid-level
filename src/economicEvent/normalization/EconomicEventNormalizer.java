package economicEvent.normalization;

public interface EconomicEventNormalizer<S> {
    EconomicEventNormalizationResult normalize(S source);
}
