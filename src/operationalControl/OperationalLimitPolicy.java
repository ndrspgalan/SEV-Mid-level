package operationalControl;

import coinProperties.Currency;
import consumableRegistry.ConsumableType;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class OperationalLimitPolicy {
    private final OperationalPolicyId id;
    private final PolicyScope scope;
    private final String scopeKey;
    private final MonetaryOperationType operationType;
    private final Optional<Currency> currency;
    private final Optional<Currency> targetCurrency;
    private final Optional<ConsumableType> consumableType;
    private final PolicyEffect effect;
    private final LimitWindow window;
    private final Optional<Integer> maximumPerOperation;
    private final Optional<Long> maximumAccumulatedAmount;
    private final Optional<Integer> maximumOperationCount;
    private final Instant effectiveFrom;
    private Instant effectiveUntil;

    public OperationalLimitPolicy(OperationalPolicyId id, PolicyScope scope, String scopeKey,
            MonetaryOperationType operationType, Optional<Currency> currency, LimitWindow window,
            Optional<Integer> maximumPerOperation, Optional<Long> maximumAccumulatedAmount,
            Optional<Integer> maximumOperationCount, Instant effectiveFrom, Instant effectiveUntil) {
        this(id, scope, scopeKey, operationType, currency, Optional.empty(), Optional.empty(),
                PolicyEffect.LIMITED, window, maximumPerOperation, maximumAccumulatedAmount,
                maximumOperationCount, effectiveFrom, effectiveUntil);
    }

    public OperationalLimitPolicy(OperationalPolicyId id, PolicyScope scope, String scopeKey,
            MonetaryOperationType operationType, Optional<Currency> currency,
            Optional<Currency> targetCurrency, Optional<ConsumableType> consumableType,
            PolicyEffect effect, LimitWindow window, Optional<Integer> maximumPerOperation,
            Optional<Long> maximumAccumulatedAmount, Optional<Integer> maximumOperationCount,
            Instant effectiveFrom, Instant effectiveUntil) {
        this.id = Objects.requireNonNull(id);
        this.scope = Objects.requireNonNull(scope);
        this.scopeKey = requireKey(scopeKey);
        this.operationType = Objects.requireNonNull(operationType);
        this.currency = Objects.requireNonNull(currency);
        this.targetCurrency = Objects.requireNonNull(targetCurrency);
        this.consumableType = Objects.requireNonNull(consumableType);
        this.effect = Objects.requireNonNull(effect);
        this.window = Objects.requireNonNull(window);
        this.maximumPerOperation = positive(maximumPerOperation, "maximumPerOperation");
        this.maximumAccumulatedAmount = positiveLong(maximumAccumulatedAmount, "maximumAccumulatedAmount");
        this.maximumOperationCount = positive(maximumOperationCount, "maximumOperationCount");
        if (effect == PolicyEffect.LIMITED && this.maximumPerOperation.isEmpty()
                && this.maximumAccumulatedAmount.isEmpty() && this.maximumOperationCount.isEmpty()) {
            throw new IllegalArgumentException("limited policy must define at least one limit");
        }
        if (effect != PolicyEffect.LIMITED && (this.maximumPerOperation.isPresent()
                || this.maximumAccumulatedAmount.isPresent() || this.maximumOperationCount.isPresent())) {
            throw new IllegalArgumentException("unlimited or denied policy cannot define numeric limits");
        }
        this.effectiveFrom = Objects.requireNonNull(effectiveFrom);
        this.effectiveUntil = effectiveUntil;
        if (effectiveUntil != null && !effectiveUntil.isAfter(effectiveFrom)) {
            throw new IllegalArgumentException("effectiveUntil must be after effectiveFrom");
        }
    }

    public static OperationalLimitPolicy create(PolicyScope scope, String scopeKey,
            MonetaryOperationType type, Currency currency, LimitWindow window,
            Integer perOperation, Long accumulated, Integer count, Instant from) {
        return limited(scope, scopeKey, type, currency, null, null, window,
                perOperation, accumulated, count, from);
    }

    public static OperationalLimitPolicy limited(PolicyScope scope, String scopeKey,
            MonetaryOperationType type, Currency currency, Currency targetCurrency,
            ConsumableType consumableType, LimitWindow window, Integer perOperation,
            Long accumulated, Integer count, Instant from) {
        return new OperationalLimitPolicy(OperationalPolicyId.generate(), scope, scopeKey, type,
                Optional.ofNullable(currency), Optional.ofNullable(targetCurrency),
                Optional.ofNullable(consumableType), PolicyEffect.LIMITED, window,
                Optional.ofNullable(perOperation), Optional.ofNullable(accumulated),
                Optional.ofNullable(count), from, null);
    }

    public static OperationalLimitPolicy denied(PolicyScope scope, String scopeKey,
            MonetaryOperationType type, Currency currency, Currency targetCurrency,
            ConsumableType consumableType, Instant from) {
        return new OperationalLimitPolicy(OperationalPolicyId.generate(), scope, scopeKey, type,
                Optional.ofNullable(currency), Optional.ofNullable(targetCurrency),
                Optional.ofNullable(consumableType), PolicyEffect.DENIED, LimitWindow.MONTHLY,
                Optional.empty(), Optional.empty(), Optional.empty(), from, null);
    }

    public static OperationalLimitPolicy unlimited(PolicyScope scope, String scopeKey,
            MonetaryOperationType type, Currency currency, Currency targetCurrency,
            ConsumableType consumableType, Instant from) {
        return new OperationalLimitPolicy(OperationalPolicyId.generate(), scope, scopeKey, type,
                Optional.ofNullable(currency), Optional.ofNullable(targetCurrency),
                Optional.ofNullable(consumableType), PolicyEffect.UNLIMITED, LimitWindow.MONTHLY,
                Optional.empty(), Optional.empty(), Optional.empty(), from, null);
    }

    private static Optional<Integer> positive(Optional<Integer> value, String field) {
        value.ifPresent(v -> { if (v <= 0) throw new IllegalArgumentException(field + " must be positive"); });
        return value;
    }
    private static Optional<Long> positiveLong(Optional<Long> value, String field) {
        value.ifPresent(v -> { if (v <= 0) throw new IllegalArgumentException(field + " must be positive"); });
        return value;
    }
    private static String requireKey(String value) {
        Objects.requireNonNull(value); String v = value.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("scopeKey must not be blank");
        return v;
    }

    public boolean isEffectiveAt(Instant at) { return !at.isBefore(effectiveFrom) && (effectiveUntil == null || at.isBefore(effectiveUntil)); }
    public void deactivateAt(Instant at) { Objects.requireNonNull(at); if (!at.isAfter(effectiveFrom)) throw new IllegalArgumentException("deactivation must be after activation"); if (effectiveUntil == null || at.isBefore(effectiveUntil)) effectiveUntil = at; }
    public OperationalPolicyId id() { return id; }
    public PolicyScope scope() { return scope; }
    public String scopeKey() { return scopeKey; }
    public MonetaryOperationType operationType() { return operationType; }
    public Optional<Currency> currency() { return currency; }
    public Optional<Currency> targetCurrency() { return targetCurrency; }
    public Optional<ConsumableType> consumableType() { return consumableType; }
    public PolicyEffect effect() { return effect; }
    public LimitWindow window() { return window; }
    public Optional<Integer> maximumPerOperation() { return maximumPerOperation; }
    public Optional<Long> maximumAccumulatedAmount() { return maximumAccumulatedAmount; }
    public Optional<Integer> maximumOperationCount() { return maximumOperationCount; }
    public Instant effectiveFrom() { return effectiveFrom; }
    public Optional<Instant> effectiveUntil() { return Optional.ofNullable(effectiveUntil); }
}
