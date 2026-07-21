package transaction;

import java.util.*;

public final class InMemoryTransactionLedger implements TransactionLedger {

    private final Map<TransactionId, TransactionRecord> records =
            new LinkedHashMap<>();

    @Override
    public synchronized void append(TransactionRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        if (records.containsKey(record.id())) {
            throw new IllegalArgumentException(
                    "transaction id already exists: " + record.id()
            );
        }
        records.put(record.id(), record);
    }

    @Override
    public synchronized Optional<TransactionRecord> findById(TransactionId id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(records.get(id));
    }

    @Override
    public synchronized List<TransactionRecord> findAll() {
        return List.copyOf(new ArrayList<>(records.values()));
    }
}
