package operationalControl;

public enum MonetaryOperationType {
    MINT("Acuñación"),
    PURCHASE("Compra"),
    SALE("Venta"),
    EXCHANGE("Intercambio"),
    TRANSFER_SENT("Transferencia enviada"),
    TRANSFER_RECEIVED("Transferencia recibida");

    private final String label;
    MonetaryOperationType(String label) { this.label = label; }
    public String label() { return label; }
}
