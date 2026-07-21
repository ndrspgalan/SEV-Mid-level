package application.operation;

import coinProperties.Currency;
import consumableRegistry.*;
import consumerRegistry.BankAccount;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;
import operationalControl.*;
import transaction.*;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class PurchaseOperationService {
    private final ConsumerRegistry consumerRegistry;
    private final ConsumableRegistry consumableRegistry;
    private final CommercialTransactionPolicy transactionPolicy;
    private final TransactionLedger transactionLedger;
    private final Clock clock;
    private final OperationalControlService operationalControlService;
    private final OperationalDecisionJournal operationalDecisionJournal;

    public PurchaseOperationService(ConsumerRegistry consumers, ConsumableRegistry consumables,
            CommercialTransactionPolicy policy, TransactionLedger ledger, Clock clock) {
        this(consumers,consumables,policy,ledger,clock,null,null);
    }
    public PurchaseOperationService(ConsumerRegistry consumers, ConsumableRegistry consumables,
            CommercialTransactionPolicy policy, TransactionLedger ledger, Clock clock,
            OperationalControlService controls, OperationalDecisionJournal decisions) {
        this.consumerRegistry=Objects.requireNonNull(consumers); this.consumableRegistry=Objects.requireNonNull(consumables);
        this.transactionPolicy=Objects.requireNonNull(policy); this.transactionLedger=Objects.requireNonNull(ledger);
        this.clock=Objects.requireNonNull(clock); this.operationalControlService=controls; this.operationalDecisionJournal=decisions;
    }

    public PurchaseOperationResult purchase(String buyerId,String sellerId,String consumableId) {
        TransactionId id=TransactionId.generate(); Instant at=Instant.now(clock);
        Optional<Consumer> buyerFound=consumerRegistry.findById(buyerId);
        if(buyerFound.isEmpty()){var r=PurchaseOperationResult.rejected(PurchaseOperationResult.RejectionReason.BUYER_NOT_FOUND);append(id,at,buyerId,sellerId,consumableId,r,Optional.empty());return r;}
        Optional<Consumer> sellerFound=consumerRegistry.findById(sellerId);
        if(sellerFound.isEmpty()){var r=PurchaseOperationResult.rejected(PurchaseOperationResult.RejectionReason.SELLER_NOT_FOUND);append(id,at,buyerId,sellerId,consumableId,r,Optional.empty());return r;}
        Optional<Consumable> itemFound=consumableRegistry.findById(consumableId);
        if(itemFound.isEmpty()){var r=PurchaseOperationResult.rejected(PurchaseOperationResult.RejectionReason.CONSUMABLE_NOT_FOUND);append(id,at,buyerId,sellerId,consumableId,r,Optional.empty());return r;}
        Consumer buyer=buyerFound.orElseThrow(),seller=sellerFound.orElseThrow(); Consumable item=itemFound.orElseThrow();
        BankAccount buyerAccount=buyer.getBankAccount(),sellerAccount=seller.getBankAccount(); Currency currency=item.getPriceCurrency(); int amount=item.getPrice();
        int buyerBefore=buyerAccount.getBalance(currency),sellerBefore=sellerAccount.getBalance(currency);
        OperationalAuthorization buyerAuth=authorize(buyerAccount,MonetaryOperationType.PURCHASE,currency,item.getType(),amount,at);
        if(buyerAuth!=null&&!buyerAuth.allowed()){
            var r=PurchaseOperationResult.policyRejected(CommercialTransactionRejectionReason.BUYER_OPERATIONAL_LIMIT_EXCEEDED);append(id,at,buyerId,sellerId,consumableId,r,Optional.of(item),buyerBefore,sellerBefore);record(id,buyer,Optional.of(seller),MonetaryOperationType.PURCHASE,currency,item.getType(),amount,buyerAuth,at);return r;
        }
        OperationalAuthorization sellerAuth=authorize(sellerAccount,MonetaryOperationType.SALE,currency,item.getType(),amount,at);
        if(sellerAuth!=null&&!sellerAuth.allowed()){
            if(buyerAuth!=null)operationalControlService.release(buyerAuth);
            var r=PurchaseOperationResult.policyRejected(CommercialTransactionRejectionReason.SELLER_OPERATIONAL_LIMIT_EXCEEDED);append(id,at,buyerId,sellerId,consumableId,r,Optional.of(item),buyerBefore,sellerBefore);
            if(buyerAuth!=null)record(id,buyer,Optional.of(seller),MonetaryOperationType.PURCHASE,currency,item.getType(),amount,buyerAuth,at);record(id,seller,Optional.of(buyer),MonetaryOperationType.SALE,currency,item.getType(),amount,sellerAuth,at);return r;
        }
        CommercialTransactionResult policyResult=transactionPolicy.purchase(buyerAccount,sellerAccount,item);
        if(!policyResult.isAccepted()){
            if(buyerAuth!=null)operationalControlService.release(buyerAuth);if(sellerAuth!=null)operationalControlService.release(sellerAuth);
            var r=PurchaseOperationResult.policyRejected(policyResult.getRejectionReason().orElseThrow());append(id,at,buyerId,sellerId,consumableId,r,Optional.of(item),buyerBefore,sellerBefore);
            if(buyerAuth!=null)record(id,buyer,Optional.of(seller),MonetaryOperationType.PURCHASE,currency,item.getType(),amount,buyerAuth,at);if(sellerAuth!=null)record(id,seller,Optional.of(buyer),MonetaryOperationType.SALE,currency,item.getType(),amount,sellerAuth,at);return r;
        }
        if(buyerAuth!=null)operationalControlService.commit(buyerAuth);if(sellerAuth!=null)operationalControlService.commit(sellerAuth);
        var r=PurchaseOperationResult.accepted(buyer.getName(),seller.getName(),item.getName(),currency,amount,buyerBefore,buyerAccount.getBalance(currency),sellerBefore,sellerAccount.getBalance(currency));
        append(id,at,buyerId,sellerId,consumableId,r,Optional.of(item),buyerBefore,sellerBefore);
        if(buyerAuth!=null)record(id,buyer,Optional.of(seller),MonetaryOperationType.PURCHASE,currency,item.getType(),amount,buyerAuth,at);if(sellerAuth!=null)record(id,seller,Optional.of(buyer),MonetaryOperationType.SALE,currency,item.getType(),amount,sellerAuth,at);return r;
    }
    private OperationalAuthorization authorize(BankAccount account, MonetaryOperationType type, Currency currency, ConsumableType consumableType, int amount, Instant at){
        return operationalControlService==null ? null : operationalControlService.authorize(
                OperationalControlRequest.commercial(account, type, currency, consumableType, amount, at));
    }
    private void record(TransactionId id,Consumer actor,Optional<Consumer> counterparty,MonetaryOperationType type,Currency currency,ConsumableType consumableType,int amount,OperationalAuthorization auth,Instant at){
        if(operationalDecisionJournal!=null) operationalDecisionJournal.append(OperationalDecisionRecord.capture(
                id,actor,counterparty,type,currency,Optional.empty(),Optional.of(consumableType),amount,auth.snapshot(),at));
    }
    private void append(TransactionId id,Instant at,String buyerId,String sellerId,String consumableId,PurchaseOperationResult result,Optional<Consumable> item,int... balances){
        Optional<String> rejection=result.getRejectionReason().map(Enum::name);if(result.getPolicyRejectionReason().isPresent())rejection=Optional.of("TRANSACTION_POLICY_REJECTION:"+result.getPolicyRejectionReason().orElseThrow().name());
        Optional<Integer> buyerBefore=balances.length>=1?Optional.of(balances[0]):Optional.empty(),sellerBefore=balances.length>=2?Optional.of(balances[1]):Optional.empty();
        transactionLedger.append(new TransactionRecord(id,at,TransactionType.PURCHASE,result.isAccepted()?TransactionStatus.COMPLETED:TransactionStatus.REJECTED,
                new PurchaseTransactionDetails(buyerId,sellerId,consumableId,item.map(Consumable::getName),item.map(Consumable::getCategory),item.map(value -> 1),item.map(Consumable::getPriceCurrency),item.map(Consumable::getPrice),item.map(Consumable::getPrice),buyerBefore,result.isAccepted()?Optional.of(result.getBuyerBalanceAfter()):Optional.empty(),sellerBefore,result.isAccepted()?Optional.of(result.getSellerBalanceAfter()):Optional.empty(),rejection)));
    }
}
