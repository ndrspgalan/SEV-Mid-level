package tests;

import application.operation.ExchangeOperationService;
import banking.census.ProfessionCatalog;
import banking.census.ProfessionCensus;
import coinProperties.Currency;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;
import exchangeCoin.ImplementedExchangePolicy;
import operationalControl.*;
import transaction.InMemoryTransactionLedger;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class OperationalControlTest {
    private static final Instant NOW=Instant.parse("1456-01-30T10:00:00Z");
    public static void main(String[] args){
        perOperationAndPeriodLimitsAreEnforced();
        rejectedOperationsDoNotConsumeUsage();
        accountPolicyPrecedesProfessionAndBankPolicy();
        periodsResetNaturally();
        exchangeIntegrationPersistsDecisionSnapshot();
        transferReplayDoesNotConsumeTwice();
        labelsAreReadable();
        System.out.println("OperationalControlTest: OK");
    }
    private static ConsumerRegistry registry(){return new ConsumerRegistry(ProfessionCatalog.valerianStandard(),new ProfessionCensus(),new accountHistory.InMemoryAccountHistoryJournal(),Clock.fixed(NOW,ZoneOffset.UTC));}
    private static void perOperationAndPeriodLimitsAreEnforced(){
        Consumer c=registry().register("Álvaro","Carpintero"); OperationalPolicyRegistry policies=new OperationalPolicyRegistry();
        policies.register(OperationalLimitPolicy.create(PolicyScope.PROFESSION,"Carpintero",MonetaryOperationType.EXCHANGE,Currency.SUELDO,LimitWindow.DAILY,100,150L,2,NOW.minusSeconds(1)));
        OperationalControlService service=new OperationalControlService(policies,ZoneOffset.UTC);
        var first=service.authorize(new OperationalControlRequest(c.getBankAccount(),MonetaryOperationType.EXCHANGE,Currency.SUELDO,80,NOW)); check(first.allowed(),"first allowed"); service.commit(first);
        var second=service.authorize(new OperationalControlRequest(c.getBankAccount(),MonetaryOperationType.EXCHANGE,Currency.SUELDO,80,NOW));
        check(second.rejectionReason().orElseThrow()==OperationalControlRejectionReason.PERIOD_AMOUNT_LIMIT_EXCEEDED,"daily amount rejected");
        var single=service.authorize(new OperationalControlRequest(c.getBankAccount(),MonetaryOperationType.EXCHANGE,Currency.SUELDO,101,NOW));
        check(single.rejectionReason().orElseThrow()==OperationalControlRejectionReason.PER_OPERATION_LIMIT_EXCEEDED,"single amount rejected");
    }
    private static void rejectedOperationsDoNotConsumeUsage(){
        Consumer c=registry().register("María Luisa","Comerciante"); OperationalPolicyRegistry p=new OperationalPolicyRegistry();
        p.register(OperationalLimitPolicy.create(PolicyScope.BANK,"*",MonetaryOperationType.PURCHASE,Currency.VALERITA,LimitWindow.DAILY,null,100L,1,NOW.minusSeconds(1)));
        OperationalControlService s=new OperationalControlService(p,ZoneOffset.UTC);
        var authorization=s.authorize(new OperationalControlRequest(c.getBankAccount(),MonetaryOperationType.PURCHASE,Currency.VALERITA,50,NOW)); s.release(authorization);
        var retry=s.authorize(new OperationalControlRequest(c.getBankAccount(),MonetaryOperationType.PURCHASE,Currency.VALERITA,50,NOW)); check(retry.allowed(),"released reservation consumes nothing"); s.commit(retry);
        check(s.usageFor(c.getBankAccount(),NOW).get(0).operationCount()==1,"only completed usage counted");
    }
    private static void accountPolicyPrecedesProfessionAndBankPolicy(){
        Consumer c=registry().register("Juan-Pablo","Jornalero"); OperationalPolicyRegistry p=new OperationalPolicyRegistry();
        p.register(OperationalLimitPolicy.create(PolicyScope.BANK,"*",MonetaryOperationType.TRANSFER_SENT,Currency.VALERITA,LimitWindow.DAILY,1000,null,null,NOW.minusSeconds(1)));
        p.register(OperationalLimitPolicy.create(PolicyScope.PROFESSION,"Jornalero",MonetaryOperationType.TRANSFER_SENT,Currency.VALERITA,LimitWindow.DAILY,500,null,null,NOW.minusSeconds(1)));
        p.register(OperationalLimitPolicy.create(PolicyScope.ACCOUNT,c.getBankAccount().getBankAccountId().toString(),MonetaryOperationType.TRANSFER_SENT,Currency.VALERITA,LimitWindow.DAILY,100,null,null,NOW.minusSeconds(1)));
        var result=new OperationalControlService(p,ZoneOffset.UTC).authorize(new OperationalControlRequest(c.getBankAccount(),MonetaryOperationType.TRANSFER_SENT,Currency.VALERITA,101,NOW));
        check(!result.allowed(),"account policy wins"); check(result.snapshot().appliedPolicyIds().size()==1,"only most specific scope applied");
    }
    private static void periodsResetNaturally(){
        Consumer c=registry().register("Lucía","Carpintero"); OperationalPolicyRegistry p=new OperationalPolicyRegistry();
        p.register(OperationalLimitPolicy.create(PolicyScope.BANK,"*",MonetaryOperationType.EXCHANGE,Currency.SUELDO,LimitWindow.DAILY,null,50L,null,NOW.minusSeconds(1)));
        OperationalControlService s=new OperationalControlService(p,ZoneOffset.UTC); var first=s.authorize(new OperationalControlRequest(c.getBankAccount(),MonetaryOperationType.EXCHANGE,Currency.SUELDO,50,NOW));s.commit(first);
        var tomorrow=s.authorize(new OperationalControlRequest(c.getBankAccount(),MonetaryOperationType.EXCHANGE,Currency.SUELDO,50,NOW.plus(1,java.time.temporal.ChronoUnit.DAYS))); check(tomorrow.allowed(),"new day resets usage");
    }
    private static void exchangeIntegrationPersistsDecisionSnapshot(){
        ConsumerRegistry r=registry(); Consumer c=r.register("Daniel","Comerciante"); c.getBankAccount().deposit(Currency.SUELDO,10);
        OperationalPolicyRegistry p=new OperationalPolicyRegistry(); p.register(OperationalLimitPolicy.create(PolicyScope.PROFESSION,"Comerciante",MonetaryOperationType.EXCHANGE,Currency.SUELDO,LimitWindow.DAILY,1,10L,10,NOW.minusSeconds(1)));
        OperationalDecisionJournal journal=new OperationalDecisionJournal(); InMemoryTransactionLedger ledger=new InMemoryTransactionLedger();
        ExchangeOperationService exchange=new ExchangeOperationService(r,new ImplementedExchangePolicy(),ledger,Clock.fixed(NOW,ZoneOffset.UTC),new OperationalControlService(p,ZoneOffset.UTC),journal);
        var rejected=exchange.exchange(c.getConsumerId(),Currency.SUELDO,Currency.VALERITA,2); check(!rejected.isAccepted(),"integrated limit rejects"); check(c.getBankAccount().getBalance(Currency.SUELDO)==10,"balance unchanged");
        check(journal.findAll().size()==1,"decision snapshot persisted"); check(!journal.findAll().get(0).snapshot().allowed(),"rejected snapshot retained");
    }

    private static void transferReplayDoesNotConsumeTwice(){
        ConsumerRegistry r=registry(); Consumer source=r.register("Elena","Carpintero"),destination=r.register("Tomás","Comerciante"); source.getBankAccount().deposit(Currency.VALERITA,100);
        OperationalPolicyRegistry p=new OperationalPolicyRegistry(); p.register(OperationalLimitPolicy.create(PolicyScope.BANK,"*",MonetaryOperationType.TRANSFER_SENT,Currency.VALERITA,LimitWindow.DAILY,100,100L,1,NOW.minusSeconds(1)));
        OperationalControlService controls=new OperationalControlService(p,ZoneOffset.UTC); OperationalDecisionJournal decisions=new OperationalDecisionJournal();
        application.operation.TransferOperationService service=new application.operation.TransferOperationService(r,new transfer.ImplementedTransferPolicy(),new transfer.InMemoryTransferRequestRegistry(),new InMemoryTransactionLedger(),Clock.fixed(NOW,ZoneOffset.UTC),controls,decisions);
        transfer.TransferRequest request=new transfer.TransferRequest(transfer.TransferRequestId.generate(),source.getConsumerId(),destination.getConsumerId(),Currency.VALERITA,50,"TEST");
        var first=service.transfer(request); var replay=service.transfer(request); check(first.isCompleted(),"first transfer completed");check(replay.isIdempotentReplay(),"transfer replayed");
        check(controls.usageFor(source.getBankAccount(),NOW).get(0).operationCount()==1,"replay does not consume twice");
        check(decisions.findAll().size()==2,"replay creates no second decision");
    }
    private static void labelsAreReadable(){check(OperationalControlRejectionReason.PER_OPERATION_LIMIT_EXCEEDED.label().startsWith("Se ha"),"label exposed");}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
