package transaction;

import java.util.List;
import java.util.Optional;

public interface TransactionLedger {

    void append(TransactionRecord record);

    Optional<TransactionRecord> findById(TransactionId id);

    List<TransactionRecord> findAll();
}
