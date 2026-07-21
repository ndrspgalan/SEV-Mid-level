package transaction;

public sealed interface TransactionDetails permits
        MintTransactionDetails,
        ExchangeTransactionDetails,
        PurchaseTransactionDetails,
        TransferTransactionDetails {

    String summary();
}
