package transaction.query;

import java.util.List;
import java.util.Objects;

public record TransactionPage<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {

    public TransactionPage {
        content = List.copyOf(Objects.requireNonNull(content));
        if (pageNumber < 0) {
            throw new IllegalArgumentException("pageNumber must not be negative");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be positive");
        }
        if (totalElements < 0 || totalPages < 0) {
            throw new IllegalArgumentException(
                    "totalElements and totalPages must not be negative"
            );
        }
    }

    public boolean hasPrevious() {
        return pageNumber > 0;
    }

    public boolean hasNext() {
        return pageNumber + 1 < totalPages;
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }
}
