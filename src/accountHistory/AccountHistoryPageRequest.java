package accountHistory;
public record AccountHistoryPageRequest(int pageNumber, int pageSize) {
    public AccountHistoryPageRequest {
        if (pageNumber < 0) throw new IllegalArgumentException("page number must not be negative");
        if (pageSize < 1 || pageSize > 100) throw new IllegalArgumentException("page size must be between 1 and 100");
    }
}
