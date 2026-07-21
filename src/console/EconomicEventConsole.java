package console;

import application.analytics.EconomicEventQueryService;
import application.analytics.EconomicEventStatisticsService;
import application.analytics.audit.EconomicEventInvariantAuditor;
import application.analytics.projection.EconomicEventProjectionResult;
import application.analytics.projection.EconomicEventProjectionService;
import application.audit.InvariantAuditReport;
import application.audit.InvariantViolation;
import economicEvent.*;
import economicEvent.query.*;

import java.io.PrintStream;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Console façade for the M1 analytical projection. */
public final class EconomicEventConsole {
    private final ConsoleInput input;
    private final PrintStream output;
    private final EconomicEventProjectionService projectionService;
    private final EconomicEventQueryService queryService;
    private final EconomicEventStatisticsService statisticsService;
    private final EconomicEventInvariantAuditor invariantAuditor;

    public EconomicEventConsole(
            ConsoleInput input,
            PrintStream output,
            EconomicEventProjectionService projectionService,
            EconomicEventQueryService queryService,
            EconomicEventStatisticsService statisticsService,
            EconomicEventInvariantAuditor invariantAuditor) {
        this.input = Objects.requireNonNull(input);
        this.output = Objects.requireNonNull(output);
        this.projectionService = Objects.requireNonNull(projectionService);
        this.queryService = Objects.requireNonNull(queryService);
        this.statisticsService = Objects.requireNonNull(statisticsService);
        this.invariantAuditor = Objects.requireNonNull(invariantAuditor);
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            int option = input.readInt("Selecciona una opción: ", 0, 6);
            output.println();
            switch (option) {
                case 1 -> projectAll();
                case 2 -> listEvents();
                case 3 -> showEventDetail();
                case 4 -> showStatistics();
                case 5 -> auditProjection();
                case 6 -> showRejectedEvents();
                case 0 -> running = false;
                default -> throw new IllegalStateException("Unexpected option");
            }
            output.println();
        }
    }

    private void printMenu() {
        output.println("=== Análisis de eventos económicos — Mid M1 ===");
        output.println("1. Proyectar todos los journals");
        output.println("2. Listar eventos económicos");
        output.println("3. Consultar evento por ID");
        output.println("4. Mostrar estadísticas descriptivas");
        output.println("5. Auditar proyección analítica");
        output.println("6. Listar eventos rechazados");
        output.println("0. Volver");
    }

    private void projectAll() {
        EconomicEventProjectionResult result = projectionService.projectAll();
        output.println("Proyección finalizada.");
        output.println("Fuentes inspeccionadas: " + result.inspected());
        output.println("Eventos creados: " + result.created());
        output.println("Eventos ya presentes: " + result.alreadyPresent());
        output.println("Fallos: " + result.failed());
        result.failures().forEach(failure -> output.println(
                "- " + failure.sourceType() + " / " + failure.sourceId() + " / "
                        + failure.reason() + " / " + failure.detail()));
    }

    private void listEvents() {
        EconomicEventQuery query = queryFor(Optional.empty());
        EconomicEventPage<EconomicEvent> page = queryService.search(query);
        printPage(page);
    }

    private void showRejectedEvents() {
        EconomicEventQuery query = queryFor(Optional.of(true));
        printPage(queryService.search(query));
    }

    private void showEventDetail() {
        String id = input.readRequiredText("ID del evento económico: ");
        queryService.findById(new EconomicEventId(id))
                .ifPresentOrElse(this::printDetail, () -> output.println("Evento no encontrado."));
    }

    private void showStatistics() {
        EconomicEventStatistics statistics = statisticsService.calculate(queryFor(Optional.empty()));
        output.println("Total de eventos: " + statistics.totalEvents());
        output.println("Eventos monetarios: " + statistics.monetaryEvents());
        output.println("Eventos rechazados: " + statistics.rejectedEvents());
        output.println("Cuentas actoras únicas: " + statistics.uniqueActorAccounts());
        output.println("Consumidores únicos: " + statistics.uniqueConsumers());
        printNonZero("Por categoría", statistics.byCategory());
        printNonZero("Por estado", statistics.byStatus());
        printNonZero("Por fuente", statistics.bySource());
        printNonZero("Volumen monetario", statistics.monetaryVolume());
    }

    private void auditProjection() {
        InvariantAuditReport report = invariantAuditor.audit();
        output.println("Auditoría: " + (report.isValid() ? "VÁLIDA" : "INVÁLIDA"));
        output.println("Instante: " + report.auditedAt());
        output.println("Violaciones: " + report.violations().size());
        for (InvariantViolation violation : report.violations()) {
            output.println("- [" + violation.code() + "] " + violation.label());
            if (!violation.context().isBlank()) output.println("  " + violation.context());
        }
    }

    private EconomicEventQuery queryFor(Optional<Boolean> rejected) {
        Optional<EconomicEventType> type = optionalEnum("Tipo de evento", EconomicEventType.values());
        Optional<EconomicEventCategory> category = optionalEnum("Categoría", EconomicEventCategory.values());
        Optional<EconomicEventStatus> status = rejected.isPresent()
                ? Optional.empty()
                : optionalEnum("Estado", EconomicEventStatus.values());
        Optional<EconomicEventSourceType> source = optionalEnum(
                "Fuente", EconomicEventSourceType.values());
        int pageNumber = input.readInt("Página (0-999): ", 0, 999);
        int pageSize = input.readInt("Tamaño de página (1-200): ", 1, 200);
        EconomicEventSortDirection direction = input.readEnumChoice(
                "Orden:", EconomicEventSortDirection.values());
        return new EconomicEventQuery(
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                type, category, status, Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), source, Optional.empty(), rejected, direction,
                new EconomicEventPageRequest(pageNumber, pageSize));
    }

    private <E extends Enum<E>> Optional<E> optionalEnum(String title, E[] values) {
        output.println(title + ":");
        output.println("0. Sin filtro");
        for (int index = 0; index < values.length; index++) {
            output.println((index + 1) + ". " + values[index]);
        }
        int choice = input.readInt("Selecciona una opción: ", 0, values.length);
        return choice == 0 ? Optional.empty() : Optional.of(values[choice - 1]);
    }

    private void printPage(EconomicEventPage<EconomicEvent> page) {
        output.println("Resultados: " + page.totalElements()
                + " | Página " + page.pageNumber() + " de " + page.totalPages());
        if (page.isEmpty()) {
            output.println("No hay eventos para los criterios seleccionados.");
            return;
        }
        for (EconomicEvent event : page.content()) {
            output.println(event.id() + " | " + event.occurredAt() + " | "
                    + event.type() + " | " + event.status() + " | "
                    + event.actor().accountId());
        }
    }

    private void printDetail(EconomicEvent event) {
        output.println("ID: " + event.id());
        output.println("Instante: " + event.occurredAt());
        output.println("Tipo: " + event.type());
        output.println("Categoría: " + event.category());
        output.println("Estado: " + event.status());
        output.println("Actor: " + event.actor().accountId() + " / " + event.actor().consumerId());
        event.actor().institutionalAccountId().ifPresent(value -> output.println("Identidad institucional: " + value));
        event.actorProfession().ifPresent(value -> output.println("Profesión histórica: " + value));
        event.counterparty().ifPresent(value -> output.println("Contraparte: " + value.accountId()));
        event.primaryAmount().ifPresent(value -> output.println("Importe primario: " + value.amount() + " " + value.currency()));
        event.secondaryAmount().ifPresent(value -> output.println("Importe secundario: " + value.amount() + " " + value.currency()));
        event.productCategory().ifPresent(value -> output.println("Categoría de producto: " + value));
        event.rejectionReason().ifPresent(value -> output.println("Motivo de rechazo: " + value));
        output.println("Fuente: " + event.source().type() + " / " + event.source().sourceId());
        event.source().sourceReference().ifPresent(value -> output.println("Referencia: " + value));
        if (!event.attributes().isEmpty()) {
            output.println("Atributos:");
            event.attributes().forEach((key, value) -> output.println("- " + key + " = " + value));
        }
    }

    private <K> void printNonZero(String title, Map<K, Long> values) {
        output.println(title + ":");
        values.forEach((key, value) -> {
            if (value != 0) output.println("- " + key + ": " + value);
        });
    }
}
