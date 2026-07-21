package operationalControl;

import transaction.TransactionId;

import java.util.*;
public final class OperationalDecisionJournal {
 private final Map<TransactionId,List<OperationalDecisionRecord>> records=new LinkedHashMap<>();
 public synchronized void append(OperationalDecisionRecord r){records.computeIfAbsent(r.transactionId(),k->new ArrayList<>()).add(Objects.requireNonNull(r));}
 public synchronized List<OperationalDecisionRecord> findByTransactionId(TransactionId id){return List.copyOf(records.getOrDefault(id,List.of()));}
 public synchronized List<OperationalDecisionRecord> findAll(){return records.values().stream().flatMap(List::stream).toList();}
}
