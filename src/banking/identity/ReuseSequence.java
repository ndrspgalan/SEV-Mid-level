package banking.identity;

public record ReuseSequence(int value) {
    public static final int MAX = 999;
    public ReuseSequence {
        if (value < 0 || value > MAX) throw new IllegalArgumentException("reuse sequence must be between 0 and 999");
    }
    public ReuseSequence next() {
        if (value == MAX) throw new IllegalStateException("census position reuse exhausted");
        return new ReuseSequence(value + 1);
    }
}
