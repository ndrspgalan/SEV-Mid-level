package application.operation;

import coinProperties.Currency;
import exchangeCoin.ExchangeRejectionReason;

import java.util.Objects;
import java.util.Optional;

public final class ExchangeOperationResult {

    public enum RejectionReason {
        CONSUMER_NOT_FOUND,
        EXCHANGE_POLICY_REJECTION
    }

    private final boolean accepted;
    private final String consumerName;
    private final Currency sourceCurrency;
    private final Currency targetCurrency;
    private final int sourceBalanceBefore;
    private final int sourceBalanceAfter;
    private final int targetBalanceBefore;
    private final int targetBalanceAfter;
    private final int targetQuantity;
    private final RejectionReason rejectionReason;
    private final ExchangeRejectionReason policyRejectionReason;

    private ExchangeOperationResult(
            boolean accepted,
            String consumerName,
            Currency sourceCurrency,
            Currency targetCurrency,
            int sourceBalanceBefore,
            int sourceBalanceAfter,
            int targetBalanceBefore,
            int targetBalanceAfter,
            int targetQuantity,
            RejectionReason rejectionReason,
            ExchangeRejectionReason policyRejectionReason
    ) {
        this.accepted = accepted;
        this.consumerName = consumerName;
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
        this.sourceBalanceBefore = sourceBalanceBefore;
        this.sourceBalanceAfter = sourceBalanceAfter;
        this.targetBalanceBefore = targetBalanceBefore;
        this.targetBalanceAfter = targetBalanceAfter;
        this.targetQuantity = targetQuantity;
        this.rejectionReason = rejectionReason;
        this.policyRejectionReason = policyRejectionReason;
    }

    public static ExchangeOperationResult consumerNotFound() {
        return rejected(RejectionReason.CONSUMER_NOT_FOUND, null);
    }

    public static ExchangeOperationResult policyRejected(
            ExchangeRejectionReason reason
    ) {
        return rejected(
                RejectionReason.EXCHANGE_POLICY_REJECTION,
                Objects.requireNonNull(reason)
        );
    }

    private static ExchangeOperationResult rejected(
            RejectionReason reason,
            ExchangeRejectionReason policyReason
    ) {
        return new ExchangeOperationResult(
                false, null, null, null,
                0, 0, 0, 0, 0,
                reason, policyReason
        );
    }

    public static ExchangeOperationResult accepted(
            String consumerName,
            Currency sourceCurrency,
            Currency targetCurrency,
            int sourceBalanceBefore,
            int sourceBalanceAfter,
            int targetBalanceBefore,
            int targetBalanceAfter,
            int targetQuantity
    ) {
        return new ExchangeOperationResult(
                true,
                consumerName,
                sourceCurrency,
                targetCurrency,
                sourceBalanceBefore,
                sourceBalanceAfter,
                targetBalanceBefore,
                targetBalanceAfter,
                targetQuantity,
                null,
                null
        );
    }

    public boolean isAccepted() { return accepted; }
    public String getConsumerName() { return consumerName; }
    public Currency getSourceCurrency() { return sourceCurrency; }
    public Currency getTargetCurrency() { return targetCurrency; }
    public int getSourceBalanceBefore() { return sourceBalanceBefore; }
    public int getSourceBalanceAfter() { return sourceBalanceAfter; }
    public int getTargetBalanceBefore() { return targetBalanceBefore; }
    public int getTargetBalanceAfter() { return targetBalanceAfter; }
    public int getTargetQuantity() { return targetQuantity; }
    public Optional<RejectionReason> getRejectionReason() { return Optional.ofNullable(rejectionReason); }
    public Optional<ExchangeRejectionReason> getPolicyRejectionReason() { return Optional.ofNullable(policyRejectionReason); }
}
