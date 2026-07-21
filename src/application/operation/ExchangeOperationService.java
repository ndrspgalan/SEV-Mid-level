package application.operation;

import coinProperties.Currency;
import consumerRegistry.BankAccount;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;
import exchangeCoin.ExchangePolicy;
import exchangeCoin.ExchangeRejectionReason;
import exchangeCoin.ExchangeResult;
import operationalControl.*;
import transaction.*;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class ExchangeOperationService {
    private final ConsumerRegistry consumerRegistry;
    private final ExchangePolicy exchangePolicy;
    private final TransactionLedger transactionLedger;
    private final Clock clock;
    private final OperationalControlService operationalControlService;
    private final OperationalDecisionJournal operationalDecisionJournal;

    public ExchangeOperationService(ConsumerRegistry consumerRegistry, ExchangePolicy exchangePolicy,
            TransactionLedger transactionLedger, Clock clock) {
        this(consumerRegistry, exchangePolicy, transactionLedger, clock, null, null);
    }

    public ExchangeOperationService(ConsumerRegistry consumerRegistry, ExchangePolicy exchangePolicy,
            TransactionLedger transactionLedger, Clock clock,
            OperationalControlService operationalControlService,
            OperationalDecisionJournal operationalDecisionJournal) {
        this.consumerRegistry=Objects.requireNonNull(consumerRegistry);
        this.exchangePolicy=Objects.requireNonNull(exchangePolicy);
        this.transactionLedger=Objects.requireNonNull(transactionLedger);
        this.clock=Objects.requireNonNull(clock);
        this.operationalControlService=operationalControlService;
        this.operationalDecisionJournal=operationalDecisionJournal;
    }

    public ExchangeOperationResult exchange(String consumerId, Currency sourceCurrency,
            Currency targetCurrency, int sourceQuantity) {
        Objects.requireNonNull(sourceCurrency); Objects.requireNonNull(targetCurrency);
        TransactionId transactionId=TransactionId.generate(); Instant occurredAt=Instant.now(clock);
        Optional<Consumer> found=consumerRegistry.findById(consumerId);
        if(found.isEmpty()) { var result=ExchangeOperationResult.consumerNotFound(); append(transactionId,occurredAt,consumerId,sourceCurrency,targetCurrency,sourceQuantity,result); return result; }
        Consumer consumer=found.orElseThrow(); BankAccount account=consumer.getBankAccount();
        int sourceBefore=account.getBalance(sourceCurrency), targetBefore=account.getBalance(targetCurrency);
        OperationalAuthorization authorization=authorize(account,sourceCurrency,targetCurrency,sourceQuantity,occurredAt);
        if(authorization!=null && !authorization.allowed()) {
            var result=ExchangeOperationResult.policyRejected(ExchangeRejectionReason.OPERATIONAL_LIMIT_EXCEEDED);
            append(transactionId,occurredAt,consumerId,sourceCurrency,targetCurrency,sourceQuantity,result,sourceBefore,targetBefore);
            recordDecision(transactionId,consumer,sourceCurrency,targetCurrency,sourceQuantity,authorization,occurredAt); return result;
        }
        ExchangeResult policyResult=exchangePolicy.exchange(account,sourceCurrency,targetCurrency,sourceQuantity);
        if(!policyResult.isAccepted()) {
            if(authorization!=null) operationalControlService.release(authorization);
            var result=ExchangeOperationResult.policyRejected(policyResult.getRejectionReason().orElseThrow());
            append(transactionId,occurredAt,consumerId,sourceCurrency,targetCurrency,sourceQuantity,result,sourceBefore,targetBefore);
            if(authorization!=null) recordDecision(transactionId,consumer,sourceCurrency,targetCurrency,sourceQuantity,authorization,occurredAt); return result;
        }
        if(authorization!=null) operationalControlService.commit(authorization);
        var result=ExchangeOperationResult.accepted(consumer.getName(),sourceCurrency,targetCurrency,sourceBefore,
                account.getBalance(sourceCurrency),targetBefore,account.getBalance(targetCurrency),policyResult.getTargetQuantity());
        append(transactionId,occurredAt,consumerId,sourceCurrency,targetCurrency,sourceQuantity,result,sourceBefore,targetBefore);
        if(authorization!=null) recordDecision(transactionId,consumer,sourceCurrency,targetCurrency,sourceQuantity,authorization,occurredAt); return result;
    }

    private OperationalAuthorization authorize(BankAccount account, Currency sourceCurrency, Currency targetCurrency, int amount, Instant at){
        if(operationalControlService==null || amount<=0) return null;
        return operationalControlService.authorize(OperationalControlRequest.exchange(account, sourceCurrency, targetCurrency, amount, at));
    }
    private void recordDecision(TransactionId id,Consumer consumer,Currency currency,Currency targetCurrency,int amount,OperationalAuthorization auth,Instant at){
        if(operationalDecisionJournal!=null) operationalDecisionJournal.append(OperationalDecisionRecord.capture(
                id,consumer,Optional.empty(),MonetaryOperationType.EXCHANGE,currency,Optional.of(targetCurrency),Optional.empty(),amount,auth.snapshot(),at));
    }
    private void append(TransactionId id,Instant at,String consumerId,Currency source,Currency target,int quantity,ExchangeOperationResult result,int... balances){
        Optional<String> rejection=result.getRejectionReason().map(Enum::name);
        if(result.getPolicyRejectionReason().isPresent()) rejection=Optional.of("EXCHANGE_POLICY_REJECTION:"+result.getPolicyRejectionReason().orElseThrow().name());
        Optional<Integer> sourceBefore=balances.length>=1?Optional.of(balances[0]):Optional.empty(); Optional<Integer> targetBefore=balances.length>=2?Optional.of(balances[1]):Optional.empty();
        transactionLedger.append(new TransactionRecord(id,at,TransactionType.EXCHANGE,result.isAccepted()?TransactionStatus.COMPLETED:TransactionStatus.REJECTED,
                new ExchangeTransactionDetails(consumerId,source,target,quantity,result.isAccepted()?Optional.of(result.getTargetQuantity()):Optional.empty(),sourceBefore,
                        result.isAccepted()?Optional.of(result.getSourceBalanceAfter()):Optional.empty(),targetBefore,result.isAccepted()?Optional.of(result.getTargetBalanceAfter()):Optional.empty(),rejection)));
    }
}
