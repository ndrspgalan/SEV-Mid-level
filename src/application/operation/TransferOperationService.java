package application.operation;

import consumerRegistry.BankAccount;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;
import operationalControl.*;
import transaction.*;
import transfer.*;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class TransferOperationService {
    private final ConsumerRegistry consumerRegistry;
    private final TransferPolicy transferPolicy;
    private final TransferRequestRegistry requestRegistry;
    private final TransactionLedger transactionLedger;
    private final Clock clock;
    private final OperationalControlService operationalControlService;
    private final OperationalDecisionJournal operationalDecisionJournal;

    public TransferOperationService(ConsumerRegistry consumers,TransferPolicy policy,TransferRequestRegistry requests,
            TransactionLedger ledger,Clock clock){this(consumers,policy,requests,ledger,clock,null,null);}
    public TransferOperationService(ConsumerRegistry consumers,TransferPolicy policy,TransferRequestRegistry requests,
            TransactionLedger ledger,Clock clock,OperationalControlService controls,OperationalDecisionJournal decisions){
        this.consumerRegistry=Objects.requireNonNull(consumers);this.transferPolicy=Objects.requireNonNull(policy);
        this.requestRegistry=Objects.requireNonNull(requests);this.transactionLedger=Objects.requireNonNull(ledger);
        this.clock=Objects.requireNonNull(clock);this.operationalControlService=controls;this.operationalDecisionJournal=decisions;
    }
    public TransferOperationResult transfer(TransferRequest request){
        Objects.requireNonNull(request,"request must not be null");
        synchronized(requestRegistry){
            Optional<ProcessedTransferRequest> existing=requestRegistry.findById(request.requestId());
            if(existing.isPresent()){ProcessedTransferRequest processed=existing.orElseThrow();if(!processed.request().equals(request))return TransferOperationResult.idempotencyConflict(request,processed.transactionId(),processed.occurredAt());return toResult(processed).asIdempotentReplay();}
            TransactionId id=TransactionId.generate();Instant at=Instant.now(clock);TransferExecution execution=executeFirstAttempt(request,id,at);
            ProcessedTransferRequest processed=new ProcessedTransferRequest(request,id,at,execution);append(processed);requestRegistry.register(processed);return toResult(processed);
        }
    }
    private TransferExecution executeFirstAttempt(TransferRequest request,TransactionId id,Instant at){
        Optional<Consumer> sourceFound=consumerRegistry.findById(request.sourceConsumerId());
        if(sourceFound.isEmpty())return TransferExecution.rejected(TransferRejectionReason.SOURCE_CONSUMER_NOT_FOUND,Optional.empty(),destinationBalance(request));
        Optional<Consumer> destinationFound=consumerRegistry.findById(request.destinationConsumerId());
        if(destinationFound.isEmpty())return TransferExecution.rejected(TransferRejectionReason.DESTINATION_CONSUMER_NOT_FOUND,Optional.of(sourceFound.orElseThrow().getBankAccount().getBalance(request.currency())),Optional.empty());
        BankAccount source=sourceFound.orElseThrow().getBankAccount(),destination=destinationFound.orElseThrow().getBankAccount();
        OperationalAuthorization sourceAuth=authorize(source,MonetaryOperationType.TRANSFER_SENT,request,at);
        if(sourceAuth!=null&&!sourceAuth.allowed()){
            record(id,sourceFound.orElseThrow(),Optional.of(destinationFound.orElseThrow()),MonetaryOperationType.TRANSFER_SENT,request,sourceAuth,at);
            return TransferExecution.rejected(TransferRejectionReason.SOURCE_OPERATIONAL_LIMIT_EXCEEDED,Optional.of(source.getBalance(request.currency())),Optional.of(destination.getBalance(request.currency())));
        }
        OperationalAuthorization destinationAuth=authorize(destination,MonetaryOperationType.TRANSFER_RECEIVED,request,at);
        if(destinationAuth!=null&&!destinationAuth.allowed()){
            if(sourceAuth!=null)operationalControlService.release(sourceAuth);
            if(sourceAuth!=null)record(id,sourceFound.orElseThrow(),Optional.of(destinationFound.orElseThrow()),MonetaryOperationType.TRANSFER_SENT,request,sourceAuth,at);record(id,destinationFound.orElseThrow(),Optional.of(sourceFound.orElseThrow()),MonetaryOperationType.TRANSFER_RECEIVED,request,destinationAuth,at);
            return TransferExecution.rejected(TransferRejectionReason.DESTINATION_OPERATIONAL_LIMIT_EXCEEDED,Optional.of(source.getBalance(request.currency())),Optional.of(destination.getBalance(request.currency())));
        }
        TransferExecution execution=transferPolicy.transfer(source,destination,request);
        if(execution.accepted()){
            if(sourceAuth!=null)operationalControlService.commit(sourceAuth);if(destinationAuth!=null)operationalControlService.commit(destinationAuth);
        }else{
            if(sourceAuth!=null)operationalControlService.release(sourceAuth);if(destinationAuth!=null)operationalControlService.release(destinationAuth);
        }
        if(sourceAuth!=null)record(id,sourceFound.orElseThrow(),Optional.of(destinationFound.orElseThrow()),MonetaryOperationType.TRANSFER_SENT,request,sourceAuth,at);if(destinationAuth!=null)record(id,destinationFound.orElseThrow(),Optional.of(sourceFound.orElseThrow()),MonetaryOperationType.TRANSFER_RECEIVED,request,destinationAuth,at);
        return execution;
    }
    private OperationalAuthorization authorize(BankAccount account,MonetaryOperationType type,TransferRequest request,Instant at){return operationalControlService==null?null:operationalControlService.authorize(new OperationalControlRequest(account,type,request.currency(),request.quantity(),at));}
    private void record(TransactionId id,Consumer actor,Optional<Consumer> counterparty,MonetaryOperationType type,TransferRequest request,OperationalAuthorization auth,Instant at){
        if(operationalDecisionJournal!=null) operationalDecisionJournal.append(OperationalDecisionRecord.capture(
                id,actor,counterparty,type,request.currency(),Optional.empty(),Optional.empty(),request.quantity(),auth.snapshot(),at));
    }
    private Optional<Integer> destinationBalance(TransferRequest request){return consumerRegistry.findById(request.destinationConsumerId()).map(c->c.getBankAccount().getBalance(request.currency()));}
    private void append(ProcessedTransferRequest processed){TransferRequest request=processed.request();TransferExecution e=processed.execution();transactionLedger.append(new TransactionRecord(processed.transactionId(),processed.occurredAt(),TransactionType.TRANSFER,e.accepted()?TransactionStatus.COMPLETED:TransactionStatus.REJECTED,new TransferTransactionDetails(request.requestId(),request.sourceConsumerId(),request.destinationConsumerId(),request.currency(),request.quantity(),request.reference(),e.sourceBalanceBefore(),e.sourceBalanceAfter(),e.destinationBalanceBefore(),e.destinationBalanceAfter(),e.rejectionReason().map(Enum::name))));}
    private TransferOperationResult toResult(ProcessedTransferRequest processed){TransferExecution e=processed.execution();if(e.accepted())return TransferOperationResult.completed(processed.request(),processed.transactionId(),processed.occurredAt(),e.sourceBalanceBefore().orElseThrow(),e.sourceBalanceAfter().orElseThrow(),e.destinationBalanceBefore().orElseThrow(),e.destinationBalanceAfter().orElseThrow());return TransferOperationResult.rejected(processed.request(),processed.transactionId(),processed.occurredAt(),e.rejectionReason().orElseThrow(),e.sourceBalanceBefore(),e.destinationBalanceBefore());}
}
