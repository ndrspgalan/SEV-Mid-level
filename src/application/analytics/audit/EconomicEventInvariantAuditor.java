package application.analytics.audit;

import accountHistory.AccountHistoryEvent;
import accountHistory.AccountHistoryJournal;
import application.audit.InvariantAuditReport;
import application.audit.InvariantViolation;
import economicEvent.EconomicEvent;
import economicEvent.EconomicEventId;
import economicEvent.EconomicEventStatus;
import economicEvent.EconomicEventType;
import economicEvent.normalization.CompositeEconomicEventNormalizer;
import economicEvent.normalization.EconomicEventNormalizationFailure;
import economicEvent.normalization.EconomicEventNormalizationResult;
import economicEvent.normalization.EconomicEventNormalizationSuccess;
import economicEvent.repository.EconomicEventRepository;
import operationalControl.OperationalDecisionJournal;
import operationalControl.OperationalDecisionRecord;
import transaction.TransactionLedger;
import transaction.TransactionRecord;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Read-only auditor for the canonical Mid analytical projection.
 *
 * <p>The auditor independently re-normalizes every Junior source, compares the resulting
 * canonical events with the analytical repository, detects missing, divergent and orphaned
 * events, and verifies repository/index consistency. It never repairs state.</p>
 */
public final class EconomicEventInvariantAuditor {
    private final TransactionLedger transactionLedger;
    private final AccountHistoryJournal accountHistoryJournal;
    private final OperationalDecisionJournal operationalDecisionJournal;
    private final EconomicEventRepository repository;
    private final CompositeEconomicEventNormalizer normalizer;
    private final Clock clock;

    public EconomicEventInvariantAuditor(
            TransactionLedger transactionLedger,
            AccountHistoryJournal accountHistoryJournal,
            OperationalDecisionJournal operationalDecisionJournal,
            EconomicEventRepository repository,
            CompositeEconomicEventNormalizer normalizer,
            Clock clock) {
        this.transactionLedger = Objects.requireNonNull(transactionLedger);
        this.accountHistoryJournal = Objects.requireNonNull(accountHistoryJournal);
        this.operationalDecisionJournal = Objects.requireNonNull(operationalDecisionJournal);
        this.repository = Objects.requireNonNull(repository);
        this.normalizer = Objects.requireNonNull(normalizer);
        this.clock = Objects.requireNonNull(clock);
    }

    public InvariantAuditReport audit() {
        List<InvariantViolation> violations = new ArrayList<>();
        Map<EconomicEventId, EconomicEvent> expected = expectedEvents(violations);
        List<EconomicEvent> stored = repository.findAll();

        auditRepositoryStructure(stored, violations);
        auditProjectionCompleteness(expected, stored, violations);
        auditStoredEventSemantics(stored, violations);

        return new InvariantAuditReport(clock.instant(), violations);
    }

    private Map<EconomicEventId, EconomicEvent> expectedEvents(List<InvariantViolation> violations) {
        Map<EconomicEventId, EconomicEvent> expected = new LinkedHashMap<>();
        List<Object> sources = new ArrayList<>();
        sources.addAll(transactionLedger.findAll());
        sources.addAll(accountHistoryJournal.findAll());
        sources.addAll(operationalDecisionJournal.findAll());

        for (Object source : sources) {
            EconomicEventNormalizationResult result;
            try {
                result = normalizer.normalize(source);
            } catch (RuntimeException exception) {
                add(violations, "ECONOMIC_EVENT_NORMALIZER_EXCEPTION",
                        "La normalización analítica no debe lanzar excepciones para una fuente soportada.",
                        sourceContext(source) + " / " + exception.getClass().getSimpleName() + ": " + safeMessage(exception));
                continue;
            }

            if (result instanceof EconomicEventNormalizationFailure failure) {
                add(violations, "ECONOMIC_EVENT_NORMALIZATION_FAILURE",
                        "Toda fuente Junior auditable debe poder producir su proyección canónica.",
                        failure.sourceType() + " / " + failure.sourceId() + " / " + failure.reason() + " / " + failure.detail());
                continue;
            }

            for (EconomicEvent event : ((EconomicEventNormalizationSuccess) result).events()) {
                EconomicEvent previous = expected.putIfAbsent(event.id(), event);
                if (previous != null && !previous.equals(event)) {
                    add(violations, "DUPLICATE_EXPECTED_ECONOMIC_EVENT_ID",
                            "Dos fuentes no pueden producir el mismo identificador analítico con contenido distinto.",
                            event.id().toString());
                }
            }
        }
        return expected;
    }

    private void auditRepositoryStructure(List<EconomicEvent> stored, List<InvariantViolation> violations) {
        if (repository.count() != stored.size()) {
            add(violations, "ECONOMIC_EVENT_REPOSITORY_COUNT_MISMATCH",
                    "El contador del repositorio debe coincidir con el número de eventos enumerados.",
                    "count=" + repository.count() + ", findAll=" + stored.size());
        }

        Set<EconomicEventId> ids = new HashSet<>();
        for (EconomicEvent event : stored) {
            if (!ids.add(event.id())) {
                add(violations, "DUPLICATE_STORED_ECONOMIC_EVENT_ID",
                        "El repositorio analítico no puede contener identificadores duplicados.", event.id().toString());
            }
            if (!repository.exists(event.id())) {
                add(violations, "ECONOMIC_EVENT_EXISTS_INDEX_MISMATCH",
                        "El índice de existencia debe reconocer todos los eventos enumerados.", event.id().toString());
            }
            EconomicEvent resolved = repository.findById(event.id()).orElse(null);
            if (!event.equals(resolved)) {
                add(violations, "ECONOMIC_EVENT_ID_INDEX_MISMATCH",
                        "La búsqueda por identificador debe resolver exactamente el evento enumerado.", event.id().toString());
            }
        }
    }

    private static void auditProjectionCompleteness(
            Map<EconomicEventId, EconomicEvent> expected,
            List<EconomicEvent> stored,
            List<InvariantViolation> violations) {
        Map<EconomicEventId, EconomicEvent> actual = new HashMap<>();
        for (EconomicEvent event : stored) actual.putIfAbsent(event.id(), event);

        for (Map.Entry<EconomicEventId, EconomicEvent> entry : expected.entrySet()) {
            EconomicEvent actualEvent = actual.get(entry.getKey());
            if (actualEvent == null) {
                add(violations, "MISSING_PROJECTED_ECONOMIC_EVENT",
                        "Toda fuente normalizable debe estar representada en el repositorio analítico.",
                        entry.getKey().toString());
            } else if (!entry.getValue().equals(actualEvent)) {
                add(violations, "DIVERGENT_PROJECTED_ECONOMIC_EVENT",
                        "El evento almacenado debe coincidir exactamente con la normalización determinista de su fuente.",
                        entry.getKey().toString());
            }
        }

        for (EconomicEvent event : stored) {
            if (!expected.containsKey(event.id())) {
                add(violations, "ORPHAN_ECONOMIC_EVENT",
                        "Todo evento analítico debe proceder de una fuente Junior actualmente auditable.",
                        event.id() + " / " + event.source().type() + " / " + event.source().sourceId());
            }
        }
    }

    private static void auditStoredEventSemantics(List<EconomicEvent> stored, List<InvariantViolation> violations) {
        for (EconomicEvent event : stored) {
            if (!event.id().equals(event.source().eventId())
                    && !event.id().value().startsWith(event.source().eventId().value().replace(":PRIMARY", ":"))) {
                add(violations, "ECONOMIC_EVENT_SOURCE_ID_MISMATCH",
                        "El identificador del evento debe derivarse de su fuente declarada.", event.id().toString());
            }

            if (event.type() == EconomicEventType.OPERATION_AUTHORIZED
                    && event.status() != EconomicEventStatus.SUCCEEDED) {
                add(violations, "AUTHORIZED_OPERATION_WITH_INVALID_STATUS",
                        "Una autorización operacional debe proyectarse como evento exitoso.", event.id().toString());
            }
            if (event.type() == EconomicEventType.OPERATION_REJECTED
                    && event.status() != EconomicEventStatus.REJECTED) {
                add(violations, "REJECTED_OPERATION_WITH_INVALID_STATUS",
                        "Una denegación operacional debe proyectarse como evento rechazado.", event.id().toString());
            }
            if (event.rejected() && event.rejectionReason().isEmpty()) {
                add(violations, "REJECTED_EVENT_WITHOUT_REASON",
                        "Todo evento rechazado debe preservar un motivo de rechazo.", event.id().toString());
            }
        }
    }

    private static String sourceContext(Object source) {
        if (source instanceof TransactionRecord record) return "TRANSACTION_LEDGER / " + record.id();
        if (source instanceof AccountHistoryEvent event) return "ACCOUNT_HISTORY_JOURNAL / " + event.eventId();
        if (source instanceof OperationalDecisionRecord decision) {
            return "OPERATIONAL_DECISION_JOURNAL / " + decision.transactionId() + " / "
                    + decision.accountId() + " / " + decision.operationType();
        }
        return source.getClass().getName();
    }

    private static String safeMessage(RuntimeException exception) {
        return exception.getMessage() == null ? "sin detalle" : exception.getMessage();
    }

    private static void add(List<InvariantViolation> target, String code, String label, String context) {
        target.add(new InvariantViolation(code, label, context));
    }
}
