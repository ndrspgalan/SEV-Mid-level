package application.control;

import consumerRegistry.BankAccount;
import operationalControl.*;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class OperationalControlQueryService {
    private final OperationalPolicyRegistry policyRegistry;
    private final OperationalControlService controlService;
    private final OperationalDecisionJournal decisionJournal;

    public OperationalControlQueryService(OperationalPolicyRegistry policyRegistry,
            OperationalControlService controlService, OperationalDecisionJournal decisionJournal) {
        this.policyRegistry=Objects.requireNonNull(policyRegistry);
        this.controlService=Objects.requireNonNull(controlService);
        this.decisionJournal=Objects.requireNonNull(decisionJournal);
    }
    public List<OperationalLimitPolicy> policies(){ return policyRegistry.all(); }
    public List<OperationalLimitPolicy> effectivePolicies(Instant at){ return policyRegistry.effectiveAt(at); }
    public List<OperationalUsage> usage(BankAccount account,Instant at){ return controlService.usageFor(account,at); }
    public List<OperationalDecisionRecord> decisions(){ return decisionJournal.findAll(); }
}
