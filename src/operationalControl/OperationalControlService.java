package operationalControl;

import coinProperties.Currency;
import consumerRegistry.BankAccount;

import java.time.ZoneId;
import java.util.*;

public final class OperationalControlService {
    private record UsageKey(String accountId, MonetaryOperationType type, Currency currency,
                            LimitWindow window, java.time.Instant start) {}
    private record Reservation(OperationalControlRequest request,
                               List<OperationalLimitPolicy> policies,
                               List<UsageKey> keys) {}
    private static final class MutableUsage { long amount; int count; }

    private final OperationalPolicyRegistry registry;
    private final ZoneId zone;
    private final Map<UsageKey, MutableUsage> committed = new HashMap<>();
    private final Map<UsageKey, MutableUsage> reserved = new HashMap<>();
    private final Map<UUID, Reservation> reservations = new HashMap<>();

    public OperationalControlService(OperationalPolicyRegistry registry, ZoneId zone) {
        this.registry = Objects.requireNonNull(registry);
        this.zone = Objects.requireNonNull(zone);
    }

    public synchronized OperationalAuthorization authorize(OperationalControlRequest request) {
        Objects.requireNonNull(request);
        if (!request.account().isOperational()) {
            return rejected(request, OperationalControlRejectionReason.ACCOUNT_NOT_OPERATIONAL,
                    List.of(), 0, 0);
        }

        List<OperationalLimitPolicy> applicable = select(request);
        if (applicable.isEmpty()) return allowedWithoutReservation(request);

        Optional<OperationalLimitPolicy> denied = applicable.stream()
                .filter(p -> p.effect() == PolicyEffect.DENIED)
                .findFirst();
        if (denied.isPresent()) {
            return rejected(request, denialReason(request), applicable, 0, 0);
        }

        List<OperationalLimitPolicy> limited = applicable.stream()
                .filter(p -> p.effect() == PolicyEffect.LIMITED)
                .toList();
        if (limited.isEmpty()) return allowedWithoutReservation(request, applicable);

        long maxBeforeAmount = 0;
        int maxBeforeCount = 0;
        List<UsageKey> keys = new ArrayList<>();
        for (OperationalLimitPolicy p : limited) {
            UsageKey key = key(request, p.window());
            keys.add(key);
            MutableUsage c = committed.getOrDefault(key, new MutableUsage());
            MutableUsage r = reserved.getOrDefault(key, new MutableUsage());
            long amountBefore = c.amount + r.amount;
            int countBefore = c.count + r.count;
            maxBeforeAmount = Math.max(maxBeforeAmount, amountBefore);
            maxBeforeCount = Math.max(maxBeforeCount, countBefore);
            if (p.maximumPerOperation().isPresent()
                    && request.amount() > p.maximumPerOperation().orElseThrow()) {
                return rejected(request,
                        OperationalControlRejectionReason.PER_OPERATION_LIMIT_EXCEEDED,
                        applicable, amountBefore, countBefore);
            }
            if (p.maximumAccumulatedAmount().isPresent()
                    && amountBefore + request.amount() > p.maximumAccumulatedAmount().orElseThrow()) {
                return rejected(request,
                        OperationalControlRejectionReason.PERIOD_AMOUNT_LIMIT_EXCEEDED,
                        applicable, amountBefore, countBefore);
            }
            if (p.maximumOperationCount().isPresent()
                    && countBefore + 1 > p.maximumOperationCount().orElseThrow()) {
                return rejected(request,
                        OperationalControlRejectionReason.PERIOD_OPERATION_COUNT_EXCEEDED,
                        applicable, amountBefore, countBefore);
            }
        }

        UUID token = UUID.randomUUID();
        for (UsageKey k : keys) {
            MutableUsage u = reserved.computeIfAbsent(k, ignored -> new MutableUsage());
            u.amount += request.amount();
            u.count++;
        }
        reservations.put(token, new Reservation(request, applicable, List.copyOf(keys)));
        return new OperationalAuthorization(token,
                new OperationalControlSnapshot(request.occurredAt(), true,
                        applicable.stream().map(OperationalLimitPolicy::id).toList(),
                        maxBeforeAmount, maxBeforeCount,
                        maxBeforeAmount + request.amount(), maxBeforeCount + 1,
                        Optional.empty()));
    }

    public synchronized void commit(OperationalAuthorization authorization) {
        Reservation reservation = reservations.remove(authorization.token());
        if (reservation == null) {
            if (authorization.snapshot().appliedPolicyIds().isEmpty()) return;
            if (authorization.allowed()) return; // denied/unlimited decisions have no reservation
            throw new IllegalStateException(OperationalControlRejectionReason.AUTHORIZATION_NOT_FOUND.label());
        }
        move(reservation, true);
    }

    public synchronized void release(OperationalAuthorization authorization) {
        Reservation reservation = reservations.remove(authorization.token());
        if (reservation != null) move(reservation, false);
    }

    private void move(Reservation reservation, boolean commit) {
        for (UsageKey k : reservation.keys()) {
            MutableUsage r = reserved.get(k);
            r.amount -= reservation.request().amount();
            r.count--;
            if (r.amount == 0 && r.count == 0) reserved.remove(k);
            if (commit) {
                MutableUsage c = committed.computeIfAbsent(k, ignored -> new MutableUsage());
                c.amount += reservation.request().amount();
                c.count++;
            }
        }
    }

    public synchronized List<OperationalUsage> usageFor(BankAccount account, java.time.Instant at) {
        List<OperationalUsage> result = new ArrayList<>();
        for (var e : committed.entrySet()) {
            UsageKey k = e.getKey();
            if (k.accountId.equals(account.getBankAccountId().toString())) {
                java.time.Instant end = k.window.endExclusive(at, zone);
                result.add(new OperationalUsage(account.getBankAccountId(), k.type, k.currency,
                        k.window, k.start, end, e.getValue().amount, e.getValue().count));
            }
        }
        return List.copyOf(result);
    }

    private OperationalAuthorization allowedWithoutReservation(OperationalControlRequest request) {
        return allowedWithoutReservation(request, List.of());
    }

    private OperationalAuthorization allowedWithoutReservation(OperationalControlRequest request,
            List<OperationalLimitPolicy> policies) {
        return new OperationalAuthorization(UUID.randomUUID(),
                new OperationalControlSnapshot(request.occurredAt(), true,
                        policies.stream().map(OperationalLimitPolicy::id).toList(),
                        0, 0, request.amount(), 1, Optional.empty()));
    }

    private OperationalAuthorization rejected(OperationalControlRequest request,
            OperationalControlRejectionReason reason,
            List<OperationalLimitPolicy> policies, long amount, int count) {
        return new OperationalAuthorization(UUID.randomUUID(),
                new OperationalControlSnapshot(request.occurredAt(), false,
                        policies.stream().map(OperationalLimitPolicy::id).toList(),
                        amount, count, amount + request.amount(), count + 1,
                        Optional.of(reason)));
    }

    private OperationalControlRejectionReason denialReason(OperationalControlRequest request) {
        if (request.operationType() == MonetaryOperationType.EXCHANGE
                && request.targetCurrency().isPresent()) {
            return OperationalControlRejectionReason.EXCHANGE_ROUTE_NOT_ALLOWED_FOR_PROFESSION;
        }
        if ((request.operationType() == MonetaryOperationType.PURCHASE
                || request.operationType() == MonetaryOperationType.SALE)
                && request.consumableType().isPresent()) {
            return OperationalControlRejectionReason.CONSUMABLE_TYPE_NOT_ALLOWED_FOR_PROFESSION;
        }
        return OperationalControlRejectionReason.CURRENCY_NOT_ALLOWED_FOR_PROFESSION;
    }

    private UsageKey key(OperationalControlRequest request, LimitWindow window) {
        return new UsageKey(request.account().getBankAccountId().toString(),
                request.operationType(), request.currency(), window,
                window.start(request.occurredAt(), zone));
    }

    private List<OperationalLimitPolicy> select(OperationalControlRequest request) {
        List<OperationalLimitPolicy> candidates = registry.effectiveAt(request.occurredAt()).stream()
                .filter(p -> p.operationType() == request.operationType())
                .filter(p -> p.currency().isEmpty() || p.currency().orElseThrow() == request.currency())
                .filter(p -> p.targetCurrency().isEmpty()
                        || request.targetCurrency().filter(c -> c == p.targetCurrency().orElseThrow()).isPresent())
                .filter(p -> p.consumableType().isEmpty()
                        || request.consumableType().filter(t -> t == p.consumableType().orElseThrow()).isPresent())
                .filter(p -> matches(p, request.account()))
                .toList();
        if (candidates.isEmpty()) return List.of();
        int rank = candidates.stream().mapToInt(p -> rank(p.scope())).max().orElse(0);
        List<OperationalLimitPolicy> sameScope = candidates.stream()
                .filter(p -> rank(p.scope()) == rank)
                .toList();
        int specificity = sameScope.stream().mapToInt(this::specificity).max().orElse(0);
        return sameScope.stream().filter(p -> specificity(p) == specificity).toList();
    }

    private int specificity(OperationalLimitPolicy policy) {
        int value = 0;
        if (policy.currency().isPresent()) value++;
        if (policy.targetCurrency().isPresent()) value++;
        if (policy.consumableType().isPresent()) value++;
        return value;
    }

    private boolean matches(OperationalLimitPolicy policy, BankAccount account) {
        return switch (policy.scope()) {
            case BANK -> policy.scopeKey().equals("*");
            case PROFESSION -> policy.scopeKey().equalsIgnoreCase(account.getProfession().name());
            case ACCOUNT -> policy.scopeKey().equals(account.getBankAccountId().toString())
                    || policy.scopeKey().equals(account.getInstitutionalAccountId().toString());
        };
    }

    private int rank(PolicyScope scope) {
        return switch (scope) { case BANK -> 1; case PROFESSION -> 2; case ACCOUNT -> 3; };
    }
}
