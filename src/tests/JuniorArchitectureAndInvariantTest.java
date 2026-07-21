package tests;

import application.ValerianEconomicSystem;
import application.ValerianEconomicSystemBootstrap;
import application.audit.JuniorSystemInvariantAuditor;
import application.lifecycle.AccountLifecycleService;
import banking.lifecycle.AccountClosureReason;
import banking.lifecycle.AccountLifecycleAction;
import banking.lifecycle.AccountLifecycleRequest;
import banking.lifecycle.AccountLifecycleRequestId;
import coinProperties.Currency;
import consumerRegistry.Consumer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public final class JuniorArchitectureAndInvariantTest {
    public static void main(String[] args) {
        auditAcceptsCanonicalBootstrap();
        auditAcceptsLifecycleTransitions();
        System.out.println("JuniorArchitectureAndInvariantTest: OK");
    }

    private static void auditAcceptsCanonicalBootstrap() {
        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();
        var report = new JuniorSystemInvariantAuditor(fixedClock()).audit(system);
        check(report.isValid(), "canonical bootstrap must satisfy all Junior invariants: " + report.violations());
        check(report.auditedAt().equals(fixedClock().instant()), "audit time must be deterministic");
    }

    private static void auditAcceptsLifecycleTransitions() {
        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();
        Consumer consumer = system.getConsumerRegistry().register("Irene", "Cantero");
        consumer.getBankAccount().deposit(Currency.VALERITA, 7);
        consumer.getBankAccount().withdraw(Currency.VALERITA, 7);
        new AccountLifecycleService(system.getConsumerRegistry()).process(new AccountLifecycleRequest(
                AccountLifecycleRequestId.generate(), consumer.getConsumerId(), AccountLifecycleAction.CLOSE,
                AccountClosureReason.VOLUNTARY, "J8_CANONICAL_CLOSE"));
        var report = new JuniorSystemInvariantAuditor(fixedClock()).audit(system);
        check(report.isValid(), "valid closure must preserve system invariants: " + report.violations());
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.parse("1456-01-30T12:00:00Z"), ZoneOffset.UTC);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
