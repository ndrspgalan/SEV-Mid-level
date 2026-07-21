package accountHistory;

import java.util.List;

public record AccountHistoryPage(List<AccountHistoryEvent> content, int pageNumber, int pageSize,
                                 long totalElements, int totalPages) {
    public AccountHistoryPage { content = List.copyOf(content); }
    public boolean hasNext() { return pageNumber + 1 < totalPages; }
    public boolean hasPrevious() { return pageNumber > 0; }
}
