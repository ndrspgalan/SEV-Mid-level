package operationalControl;

public enum OperationalControlRejectionReason {
    OPERATION_NOT_ALLOWED_FOR_PROFESSION("La operación no está autorizada para el perfil crediticio de esta profesión."),
    CURRENCY_NOT_ALLOWED_FOR_PROFESSION("La moneda no está autorizada para el perfil crediticio de esta profesión."),
    EXCHANGE_ROUTE_NOT_ALLOWED_FOR_PROFESSION("La ruta de intercambio no está autorizada para esta profesión."),
    CONSUMABLE_TYPE_NOT_ALLOWED_FOR_PROFESSION("El tipo de producto no está autorizado para esta profesión."),
    PER_OPERATION_LIMIT_EXCEEDED("Se ha superado el importe máximo permitido para esta operación."),
    PERIOD_AMOUNT_LIMIT_EXCEEDED("Se ha superado el importe acumulado permitido para este periodo."),
    PERIOD_OPERATION_COUNT_EXCEEDED("Se ha alcanzado el número máximo de operaciones permitido para este periodo."),
    ACCOUNT_NOT_OPERATIONAL("La cuenta no se encuentra operativa."),
    AUTHORIZATION_NOT_FOUND("La autorización operativa no existe o ya fue resuelta.");

    private final String label;
    OperationalControlRejectionReason(String label) { this.label = label; }
    public String label() { return label; }
}
