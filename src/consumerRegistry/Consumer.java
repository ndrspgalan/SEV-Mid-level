package consumerRegistry;

import banking.identity.ConsumerId;
import banking.identity.PersonName;

import java.util.Objects;

public final class Consumer {
    private final ConsumerId consumerId;
    private final PersonName name;
    private final BankAccount bankAccount;

    public Consumer(ConsumerId consumerId, PersonName name, BankAccount bankAccount) {
        this.consumerId = Objects.requireNonNull(consumerId);
        this.name = Objects.requireNonNull(name);
        this.bankAccount = Objects.requireNonNull(bankAccount);
    }
    public ConsumerId getStableConsumerId() { return consumerId; }
    /** Compatibility accessor: economic operations address the current institutional account id. */
    public String getConsumerId() { return bankAccount.getInstitutionalAccountId().toString(); }
    public String getName() { return name.toString(); }
    public PersonName getPersonName() { return name; }
    public String getProfession() { return bankAccount.getProfession().name(); }
    public BankAccount getBankAccount() { return bankAccount; }
}
