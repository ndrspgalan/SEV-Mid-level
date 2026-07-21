package application.operation;

import coinProperties.Currency;
import coinProperties.Material;
import coinProperties.SealType;
import coinProperties.Weight;
import consumerRegistry.BankAccount;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;
import mintCoin.MintPolicy;
import mintCoin.MintRejectionReason;
import mintCoin.MintResult;
import operationalControl.*;
import transaction.*;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class MintOperationService {
    private final MintPolicy mintPolicy; private final TransactionLedger transactionLedger; private final Clock clock;
    private final ConsumerRegistry consumerRegistry; private final OperationalControlService operationalControlService;
    private final OperationalDecisionJournal operationalDecisionJournal;
    public MintOperationService(MintPolicy policy,TransactionLedger ledger,Clock clock){this(policy,ledger,clock,null,null,null);}
    public MintOperationService(MintPolicy policy,TransactionLedger ledger,Clock clock,ConsumerRegistry consumers,
            OperationalControlService controls,OperationalDecisionJournal decisions){
        this.mintPolicy=Objects.requireNonNull(policy);this.transactionLedger=Objects.requireNonNull(ledger);this.clock=Objects.requireNonNull(clock);
        this.consumerRegistry=consumers;this.operationalControlService=controls;this.operationalDecisionJournal=decisions;
    }
    public MintOperationResult mint(Currency currency,Material material,Weight weight,SealType seal,int grams,double copper,double silver,double gold){
        return mintInternal(null,currency,material,weight,seal,grams,copper,silver,gold);
    }
    public MintOperationResult mint(String consumerId,Currency currency,Material material,Weight weight,SealType seal,int grams,double copper,double silver,double gold){
        if(consumerRegistry==null)throw new IllegalStateException("consumer-aware minting requires operational-control dependencies");
        Optional<Consumer> found=consumerRegistry.findById(consumerId);
        if(found.isEmpty()){
            TransactionId id=TransactionId.generate();Instant at=Instant.now(clock);var result=MintOperationResult.rejected(MintOperationResult.RejectionReason.CONSUMER_NOT_FOUND);
            append(id,at,Optional.empty(),currency,material,weight,seal,grams,copper,silver,gold,result);return result;
        }
        return mintInternal(found.orElseThrow(),currency,material,weight,seal,grams,copper,silver,gold);
    }
    private MintOperationResult mintInternal(Consumer consumer,Currency currency,Material material,Weight weight,SealType seal,int grams,double copper,double silver,double gold){
        Objects.requireNonNull(currency);Objects.requireNonNull(material);Objects.requireNonNull(weight);Objects.requireNonNull(seal);
        TransactionId id=TransactionId.generate();Instant at=Instant.now(clock);
        if(!material.matchesComposition(copper,silver,gold)){var r=MintOperationResult.rejected(MintOperationResult.RejectionReason.INVALID_MATERIAL_COMPOSITION);append(id,at,consumerId(consumer),currency,material,weight,seal,grams,copper,silver,gold,r);return r;}
        if(grams<=0||grams<weight.getGrams()){var r=MintOperationResult.rejected(MintOperationResult.RejectionReason.INSUFFICIENT_METAL);append(id,at,consumerId(consumer),currency,material,weight,seal,grams,copper,silver,gold,r);return r;}
        int quantity=grams/weight.getGrams(); OperationalAuthorization auth=null; BankAccount account=consumer==null?null:consumer.getBankAccount();
        if(account!=null&&operationalControlService!=null){auth=operationalControlService.authorize(new OperationalControlRequest(account,MonetaryOperationType.MINT,currency,quantity,at));
            if(!auth.allowed()){var r=MintOperationResult.policyRejected(MintRejectionReason.OPERATIONAL_LIMIT_EXCEEDED);append(id,at,consumerId(consumer),currency,material,weight,seal,grams,copper,silver,gold,r);record(id,consumer,currency,quantity,auth,at);return r;}}
        MintResult policy=mintPolicy.mint(currency,material,weight,seal);
        if(!policy.isAccepted()){if(auth!=null)operationalControlService.release(auth);var r=MintOperationResult.policyRejected(policy.getRejectionReason().orElseThrow());append(id,at,consumerId(consumer),currency,material,weight,seal,grams,copper,silver,gold,r);if(auth!=null)record(id,consumer,currency,quantity,auth,at);return r;}
        if(auth!=null)operationalControlService.commit(auth);var r=MintOperationResult.accepted(currency,quantity,grams%weight.getGrams());append(id,at,consumerId(consumer),currency,material,weight,seal,grams,copper,silver,gold,r);if(auth!=null)record(id,consumer,currency,quantity,auth,at);return r;
    }
    private void record(TransactionId id,Consumer consumer,Currency currency,int amount,OperationalAuthorization auth,Instant at){
        if(operationalDecisionJournal!=null) operationalDecisionJournal.append(OperationalDecisionRecord.capture(
                id,consumer,Optional.empty(),MonetaryOperationType.MINT,currency,Optional.empty(),Optional.empty(),amount,auth.snapshot(),at));
    }
    private Optional<String> consumerId(Consumer consumer){return consumer==null?Optional.empty():Optional.of(consumer.getConsumerId());}
    private void append(TransactionId id,Instant at,Optional<String> consumerId,Currency currency,Material material,Weight weight,SealType seal,int grams,double copper,double silver,double gold,MintOperationResult result){
        Optional<String> rejection=result.getRejectionReason().map(Enum::name);if(result.getPolicyRejectionReason().isPresent())rejection=Optional.of("MINT_POLICY_REJECTION:"+result.getPolicyRejectionReason().orElseThrow().name());
        transactionLedger.append(new TransactionRecord(id,at,TransactionType.MINT,result.isAccepted()?TransactionStatus.COMPLETED:TransactionStatus.REJECTED,new MintTransactionDetails(consumerId,currency,material,weight,seal,grams,copper,silver,gold,result.isAccepted()?Optional.of(result.getCoinQuantity()):Optional.empty(),result.isAccepted()?Optional.of(result.getRemainingGrams()):Optional.empty(),rejection)));
    }
}
