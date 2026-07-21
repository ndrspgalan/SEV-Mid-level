package coinProperties;

public enum Currency {

    VALERITA("valerita", "valeritas"),
    SUELDO("sueldo", "sueldos"),
    BERYLARE("berylare", "berylares"),
    REAL_A5("real de A5", "reales de A5");

    private final String singular;
    private final String plural;


    Currency(String singular, String plural) {

        this.singular = singular;
        this.plural = plural;

    }


    public String getNameForQuantity (int quantity){
        return quantity == 1 ? singular : plural;
    }

}
