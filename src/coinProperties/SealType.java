package coinProperties;

public enum SealType {

    V('V', Currency.VALERITA),
    S('S', Currency.SUELDO),
    B('B', Currency.BERYLARE),
    A5('A', Currency.REAL_A5);

    private final char seal;
    private final Currency currency;

    SealType(char seal, Currency currency) {
        this.seal = seal;
        this.currency = currency;
    }

    public char getSeal() {
        return seal;
    }
    public Currency getCurrency() {
        return currency;
    }
}


