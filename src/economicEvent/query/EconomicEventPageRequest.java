package economicEvent.query;

public record EconomicEventPageRequest(int pageNumber, int pageSize) {
    public static final int MAX_PAGE_SIZE = 200;

    public EconomicEventPageRequest {
        if (pageNumber < 0) throw new IllegalArgumentException("pageNumber must not be negative");
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("pageSize must be between 1 and " + MAX_PAGE_SIZE);
        }
    }

    public static EconomicEventPageRequest firstPage(int pageSize) {
        return new EconomicEventPageRequest(0, pageSize);
    }

    public int offset() { return Math.multiplyExact(pageNumber, pageSize); }
}
