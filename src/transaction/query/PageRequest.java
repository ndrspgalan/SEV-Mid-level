package transaction.query;

public record PageRequest(int pageNumber, int pageSize) {

    public static final int MAX_PAGE_SIZE = 100;

    public PageRequest {
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must not be negative");
        }
        if (pageSize < 1 || pageSize > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException(
                    "pageSize must be between 1 and " + MAX_PAGE_SIZE
            );
        }
    }

    public static PageRequest firstPage(int pageSize) {
        return new PageRequest(0, pageSize);
    }

    public int offset() {
        return Math.multiplyExact(pageNumber, pageSize);
    }
}
