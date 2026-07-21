package coinProperties;

public enum Weight {


    ONE(1, "un gramo"),
    TWO(2, "dos gramos"),
    FIVE(5, "cinco gramos"),
    TEN(10, "diez gramos"),
    TWENTY(20, "veinte gramos"),
    FIFTY(50, "cincuenta gramos"),
    HUNDRED(100, "cien gramos");

    private final int grams;
    private final String weightLabel;

    Weight(int grams, String weightLabel){
        this.grams = grams;
        this.weightLabel = weightLabel;
    }

    public int getGrams(){
        return grams;
    }

    public String getWeightLabel(){
        return this.weightLabel;
    }
}

