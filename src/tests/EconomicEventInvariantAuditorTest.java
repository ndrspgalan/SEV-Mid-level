package tests;

import application.ValerianEconomicSystem;
import application.ValerianEconomicSystemBootstrap;
import application.audit.InvariantAuditReport;

/** Verifies projection completeness, idempotency and audit integration for Mid M1.7. */
public final class EconomicEventInvariantAuditorTest {
    private EconomicEventInvariantAuditorTest() {}

    public static void main(String[] args) {
        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();

        InvariantAuditReport beforeProjection = system.getEconomicEventInvariantAuditor().audit();
        check(!beforeProjection.isValid(), "an unprojected system must fail analytical completeness audit");
        check(beforeProjection.violations().stream()
                        .anyMatch(v -> v.code().equals("MISSING_PROJECTED_ECONOMIC_EVENT")),
                "audit must report missing canonical events");

        var projection = system.getEconomicEventProjectionService().projectAll();
        check(projection.failures().isEmpty(), "projection must complete without failures");

        InvariantAuditReport afterProjection = system.getEconomicEventInvariantAuditor().audit();
        check(afterProjection.isValid(), "fully projected bootstrap system must satisfy analytical invariants: "
                + afterProjection.violations());
        afterProjection.requireValid();

        system.getEconomicEventProjectionService().projectAll();
        InvariantAuditReport afterIdempotentReplay = system.getEconomicEventInvariantAuditor().audit();
        check(afterIdempotentReplay.isValid(), "idempotent replay must preserve analytical validity");

        System.out.println("EconomicEventInvariantAuditorTest: PASSED");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
