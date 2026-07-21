package application.operation;

import application.view.TransactionDetailView;
import application.view.TransactionSummary;
import transaction.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class TransactionViewMapper {

    TransactionSummary toSummary(TransactionRecord record) {
        return new TransactionSummary(
                record.id(),
                record.occurredAt(),
                record.type(),
                record.status(),
                record.details().summary()
        );
    }

    TransactionDetailView toDetail(TransactionRecord record) {
        return new TransactionDetailView(
                record.id(),
                record.occurredAt(),
                record.type(),
                record.status(),
                record.details().summary(),
                participantIds(record.details()),
                attributes(record.details())
        );
    }

    boolean hasParticipant(TransactionRecord record, String participantId) {
        return participantIds(record.details()).contains(participantId);
    }

    private List<String> participantIds(TransactionDetails details) {
        if (details instanceof ExchangeTransactionDetails exchange) {
            return List.of(exchange.consumerId());
        }
        if (details instanceof PurchaseTransactionDetails purchase) {
            List<String> participants = new ArrayList<>();
            participants.add(purchase.buyerId());
            if (!purchase.sellerId().equals(purchase.buyerId())) {
                participants.add(purchase.sellerId());
            }
            return List.copyOf(participants);
        }
        if (details instanceof TransferTransactionDetails transfer) {
            if (transfer.sourceConsumerId().equals(transfer.destinationConsumerId())) {
                return List.of(transfer.sourceConsumerId());
            }
            return List.of(
                    transfer.sourceConsumerId(),
                    transfer.destinationConsumerId()
            );
        }
        return List.of();
    }

    private Map<String, String> attributes(TransactionDetails details) {
        Map<String, String> attributes = new LinkedHashMap<>();

        if (details instanceof MintTransactionDetails mint) {
            attributes.put("currency", mint.currency().name());
            attributes.put("material", mint.material().name());
            attributes.put("coinWeight", mint.coinWeight().name());
            attributes.put("sealType", mint.sealType().name());
            attributes.put("totalWeightInGrams", String.valueOf(mint.totalWeightInGrams()));
            attributes.put("copperRatio", String.valueOf(mint.copperRatio()));
            attributes.put("silverRatio", String.valueOf(mint.silverRatio()));
            attributes.put("goldRatio", String.valueOf(mint.goldRatio()));
            putOptional(attributes, "coinQuantity", mint.coinQuantity());
            putOptional(attributes, "remainingGrams", mint.remainingGrams());
            putOptional(attributes, "rejectionCode", mint.rejectionCode());
            return attributes;
        }

        if (details instanceof ExchangeTransactionDetails exchange) {
            attributes.put("consumerId", exchange.consumerId());
            attributes.put("sourceCurrency", exchange.sourceCurrency().name());
            attributes.put("targetCurrency", exchange.targetCurrency().name());
            attributes.put("sourceQuantity", String.valueOf(exchange.sourceQuantity()));
            putOptional(attributes, "targetQuantity", exchange.targetQuantity());
            putOptional(attributes, "sourceBalanceBefore", exchange.sourceBalanceBefore());
            putOptional(attributes, "sourceBalanceAfter", exchange.sourceBalanceAfter());
            putOptional(attributes, "targetBalanceBefore", exchange.targetBalanceBefore());
            putOptional(attributes, "targetBalanceAfter", exchange.targetBalanceAfter());
            putOptional(attributes, "rejectionCode", exchange.rejectionCode());
            return attributes;
        }

        if (details instanceof PurchaseTransactionDetails purchase) {
            attributes.put("buyerId", purchase.buyerId());
            attributes.put("sellerId", purchase.sellerId());
            attributes.put("consumableId", purchase.consumableId());
            putOptional(attributes, "currency", purchase.currency());
            putOptional(attributes, "price", purchase.price());
            putOptional(attributes, "buyerBalanceBefore", purchase.buyerBalanceBefore());
            putOptional(attributes, "buyerBalanceAfter", purchase.buyerBalanceAfter());
            putOptional(attributes, "sellerBalanceBefore", purchase.sellerBalanceBefore());
            putOptional(attributes, "sellerBalanceAfter", purchase.sellerBalanceAfter());
            putOptional(attributes, "rejectionCode", purchase.rejectionCode());
            return attributes;
        }

        TransferTransactionDetails transfer = (TransferTransactionDetails) details;
        attributes.put("requestId", transfer.requestId().toString());
        attributes.put("sourceConsumerId", transfer.sourceConsumerId());
        attributes.put("destinationConsumerId", transfer.destinationConsumerId());
        attributes.put("currency", transfer.currency().name());
        attributes.put("quantity", String.valueOf(transfer.quantity()));
        attributes.put("reference", transfer.reference());
        putOptional(attributes, "sourceBalanceBefore", transfer.sourceBalanceBefore());
        putOptional(attributes, "sourceBalanceAfter", transfer.sourceBalanceAfter());
        putOptional(attributes, "destinationBalanceBefore", transfer.destinationBalanceBefore());
        putOptional(attributes, "destinationBalanceAfter", transfer.destinationBalanceAfter());
        putOptional(attributes, "rejectionCode", transfer.rejectionCode());
        return attributes;
    }

    private void putOptional(
            Map<String, String> target,
            String name,
            java.util.Optional<?> value
    ) {
        value.ifPresent(present -> target.put(name, String.valueOf(present)));
    }
}
