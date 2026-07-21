package accountHistory;

import java.util.List;
import java.util.Optional;

public interface AccountHistoryJournal {
    void append(AccountHistoryEvent event);
    Optional<AccountHistoryEvent> findById(AccountHistoryEventId id);
    List<AccountHistoryEvent> findAll();
}
