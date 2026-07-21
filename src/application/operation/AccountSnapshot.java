package application.operation;

import banking.identity.HolderStatus;
import banking.lifecycle.AccountOperationalStatus;
import coinProperties.Currency;

import java.time.Instant;
import java.util.Map;

public final class AccountSnapshot {
    private final String consumerId;
    private final String stableConsumerId;
    private final String bankAccountId;
    private final String institutionalAccountId;
    private final String consumerName;
    private final String profession;
    private final int censusPosition;
    private final int reuseSequence;
    private final HolderStatus holderStatus;
    private final AccountOperationalStatus operationalStatus;
    private final Map<Currency, Integer> balances;
    private final long professionChangeCount;
    private final long holderChangeCount;
    private final Instant lastModifiedAt;

    public AccountSnapshot(String stableConsumerId, String bankAccountId, String institutionalAccountId,
                           String consumerName, String profession, int censusPosition, int reuseSequence,
                           HolderStatus holderStatus, AccountOperationalStatus operationalStatus, Map<Currency, Integer> balances,
                           long professionChangeCount, long holderChangeCount, Instant lastModifiedAt) {
        this.consumerId = institutionalAccountId;
        this.stableConsumerId = stableConsumerId;
        this.bankAccountId = bankAccountId;
        this.institutionalAccountId = institutionalAccountId;
        this.consumerName = consumerName;
        this.profession = profession;
        this.censusPosition = censusPosition;
        this.reuseSequence = reuseSequence;
        this.holderStatus = holderStatus;
        this.operationalStatus = operationalStatus;
        this.balances = Map.copyOf(balances);
        this.professionChangeCount = professionChangeCount;
        this.holderChangeCount = holderChangeCount;
        this.lastModifiedAt = lastModifiedAt;
    }
    public String getConsumerId() { return consumerId; }
    public String getStableConsumerId() { return stableConsumerId; }
    public String getBankAccountId() { return bankAccountId; }
    public String getInstitutionalAccountId() { return institutionalAccountId; }
    public String getConsumerName() { return consumerName; }
    public String getProfession() { return profession; }
    public int getCensusPosition() { return censusPosition; }
    public int getReuseSequence() { return reuseSequence; }
    public HolderStatus getHolderStatus() { return holderStatus; }
    public AccountOperationalStatus getOperationalStatus() { return operationalStatus; }
    public Map<Currency, Integer> getBalances() { return balances; }
    public long getProfessionChangeCount() { return professionChangeCount; }
    public long getHolderChangeCount() { return holderChangeCount; }
    public java.util.Optional<Instant> getLastModifiedAt() { return java.util.Optional.ofNullable(lastModifiedAt); }
}
