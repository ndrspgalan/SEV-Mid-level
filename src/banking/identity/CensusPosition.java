package banking.identity;

public record CensusPosition(int value) implements Comparable<CensusPosition> {
    public static final int MIN = 1;
    public static final int MAX = 99_999;
    public CensusPosition {
        if (value < MIN || value > MAX) throw new IllegalArgumentException("census position must be between 00001 and 99999");
    }
    public String formatted() { return "%05d".formatted(value); }
    @Override public int compareTo(CensusPosition other) { return Integer.compare(value, other.value); }
    @Override public String toString() { return formatted(); }
}
