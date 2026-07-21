package behavior.temporal;

/** Valerian three-month seasons. Codes are domain labels, not civil calendar quarters. */
public enum Season {
    WINTER("Q1"), SPRING("Q2"), SUMMER("Q3"), AUTUMN("Q4");
    private final String code;
    Season(String code) { this.code = code; }
    public String code() { return code; }
}
