package console;

import accountHistory.*;
import application.account.AccountHolderService;
import application.account.AccountReleaseResult;
import application.account.ProfessionChangeResult;
import application.account.ProfessionChangeService;
import application.history.AccountHistoryQueryService;
import application.history.AccountHistoryStatisticsService;
import application.lifecycle.AccountLifecycleResult;
import application.lifecycle.AccountLifecycleService;
import application.operation.*;
import application.view.TransactionDetailView;
import application.view.TransactionStatistics;
import application.view.TransactionSummary;
import banking.lifecycle.AccountClosureReason;
import banking.lifecycle.AccountLifecycleAction;
import banking.lifecycle.AccountLifecycleRequest;
import banking.lifecycle.AccountLifecycleRequestId;
import coinProperties.Currency;
import coinProperties.Material;
import coinProperties.SealType;
import coinProperties.Weight;
import transaction.TransactionId;
import transaction.TransactionStatus;
import transaction.TransactionType;
import transaction.query.PageRequest;
import transaction.query.SortDirection;
import transaction.query.TransactionPage;
import transaction.query.TransactionQuery;
import transfer.TransferRequest;
import transfer.TransferRequestId;

import java.io.PrintStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class JuniorConsole {

    private final ConsoleInput input;
    private final PrintStream output;
    private final MintOperationService mintService;
    private final ExchangeOperationService exchangeService;
    private final PurchaseOperationService purchaseService;
    private final TransferOperationService transferService;
    private final AccountQueryService accountQueryService;
    private final TransactionQueryService transactionQueryService;
    private final TransactionStatisticsService transactionStatisticsService;
    private final ProfessionChangeService professionChangeService;
    private final AccountHolderService accountHolderService;
    private final AccountHistoryQueryService accountHistoryQueryService;
    private final AccountHistoryStatisticsService accountHistoryStatisticsService;
    private final AccountLifecycleService accountLifecycleService;

    public JuniorConsole(
            ConsoleInput input,
            PrintStream output,
            MintOperationService mintService,
            ExchangeOperationService exchangeService,
            PurchaseOperationService purchaseService,
            TransferOperationService transferService,
            AccountQueryService accountQueryService,
            TransactionQueryService transactionQueryService,
            TransactionStatisticsService transactionStatisticsService,
            ProfessionChangeService professionChangeService,
            AccountHolderService accountHolderService,
            AccountHistoryQueryService accountHistoryQueryService,
            AccountHistoryStatisticsService accountHistoryStatisticsService,
            AccountLifecycleService accountLifecycleService
    ) {
        this.input = Objects.requireNonNull(input);
        this.output = Objects.requireNonNull(output);
        this.mintService = Objects.requireNonNull(mintService);
        this.exchangeService = Objects.requireNonNull(exchangeService);
        this.purchaseService = Objects.requireNonNull(purchaseService);
        this.transferService = Objects.requireNonNull(transferService);
        this.accountQueryService = Objects.requireNonNull(accountQueryService);
        this.transactionQueryService = Objects.requireNonNull(transactionQueryService);
        this.transactionStatisticsService = Objects.requireNonNull(
                transactionStatisticsService
        );
        this.professionChangeService = Objects.requireNonNull(professionChangeService);
        this.accountHolderService = Objects.requireNonNull(accountHolderService);
        this.accountHistoryQueryService = Objects.requireNonNull(accountHistoryQueryService);
        this.accountHistoryStatisticsService = Objects.requireNonNull(accountHistoryStatisticsService);
        this.accountLifecycleService = Objects.requireNonNull(accountLifecycleService);
    }

    public void run() {
        output.println("=== Sistema Económico Valeriano — Nivel Junior (J6) ===");

        boolean running = true;
        while (running) {
            printMainMenu();
            int option = input.readInt("Selecciona una opción: ", 0, 10);
            output.println();

            switch (option) {
                case 1 -> executeMinting();
                case 2 -> executeExchange();
                case 3 -> executePurchase();
                case 4 -> executeTransfer();
                case 5 -> showAccount();
                case 6 -> showTransactionHistory();
                case 7 -> changeProfession();
                case 8 -> showAccountHistory();
                case 9 -> releaseAccountHolder();
                case 10 -> manageAccountLifecycle();
                case 0 -> running = false;
                default -> throw new IllegalStateException("Unexpected option");
            }
            output.println();
        }

        output.println("Aplicación finalizada.");
    }

    private void printMainMenu() {
        output.println("1. Acuñar moneda");
        output.println("2. Intercambiar moneda");
        output.println("3. Comprar consumible");
        output.println("4. Transferir fondos");
        output.println("5. Consultar cuenta");
        output.println("6. Consultar historial de operaciones");
        output.println("7. Cambiar profesión de una cuenta");
        output.println("8. Consultar historial longitudinal de cuenta");
        output.println("9. Dar de baja al titular de una cuenta");
        output.println("10. Gestionar ciclo de vida operativo");
        output.println("0. Salir");
    }

    private void executeMinting() {
        Currency currency = input.readEnumChoice(
                "Moneda:", Currency.values()
        );
        Material material = input.readEnumChoice(
                "Material:", Material.values()
        );
        Weight weight = input.readEnumChoice(
                "Peso facial y físico de cada moneda:", Weight.values()
        );
        SealType sealType = input.readEnumChoice(
                "Sello:", SealType.values()
        );
        int totalWeight = input.readPositiveInt(
                "Peso total disponible en gramos: "
        );
        double copper = input.readRatio("Proporción de cobre: ");
        double silver = input.readRatio("Proporción de plata: ");
        double gold = input.readRatio("Proporción de oro: ");

        if (!confirmExecution()) {
            output.println("Operación cancelada.");
            return;
        }

        MintOperationResult result = mintService.mint(
                currency,
                material,
                weight,
                sealType,
                totalWeight,
                copper,
                silver,
                gold
        );

        if (result.isAccepted()) {
            output.println("Operación realizada con éxito.");
            output.println(
                    result.getCoinQuantity()
                            + " "
                            + result.getCurrency().getNameForQuantity(
                                    result.getCoinQuantity()
                            )
            );
            output.println(result.getRemainingGrams() + " gramos sobrantes.");
            return;
        }

        output.println("Operación rechazada.");
        output.println("Motivo: " + mintRejectionMessage(result));
    }

    private void executeExchange() {
        String consumerId = input.readRequiredText("ID institucional, de persona o de cuenta: ");
        Currency source = input.readEnumChoice(
                "Moneda de origen:", Currency.values()
        );
        Currency target = input.readEnumChoice(
                "Moneda de destino:", Currency.values()
        );
        int quantity = input.readPositiveInt("Cantidad de origen: ");

        if (!confirmExecution()) {
            output.println("Operación cancelada.");
            return;
        }

        ExchangeOperationResult result = exchangeService.exchange(
                consumerId,
                source,
                target,
                quantity
        );

        if (result.isAccepted()) {
            output.println("Operación realizada con éxito.");
            output.println(
                    result.getConsumerName()
                            + " recibió "
                            + result.getTargetQuantity()
                            + " "
                            + result.getTargetCurrency().getNameForQuantity(
                                    result.getTargetQuantity()
                            )
                            + "."
            );
            printBalanceChange(
                    result.getSourceCurrency(),
                    result.getSourceBalanceBefore(),
                    result.getSourceBalanceAfter()
            );
            printBalanceChange(
                    result.getTargetCurrency(),
                    result.getTargetBalanceBefore(),
                    result.getTargetBalanceAfter()
            );
            return;
        }

        output.println("Operación rechazada.");
        output.println("Motivo: " + exchangeRejectionMessage(result));
    }

    private void executePurchase() {
        String buyerId = input.readRequiredText("ID del comprador: ");
        String sellerId = input.readRequiredText("ID del vendedor: ");
        String consumableId = input.readRequiredText("ID del consumible: ");

        if (!confirmExecution()) {
            output.println("Operación cancelada.");
            return;
        }

        PurchaseOperationResult result = purchaseService.purchase(
                buyerId,
                sellerId,
                consumableId
        );

        if (result.isAccepted()) {
            output.println("Operación realizada con éxito.");
            output.println(
                    result.getBuyerName()
                            + " compró "
                            + result.getConsumableName()
                            + " a "
                            + result.getSellerName()
                            + " por "
                            + result.getPrice()
                            + " "
                            + result.getCurrency().getNameForQuantity(
                                    result.getPrice()
                            )
                            + "."
            );
            output.println("Comprador:");
            printBalanceChange(
                    result.getCurrency(),
                    result.getBuyerBalanceBefore(),
                    result.getBuyerBalanceAfter()
            );
            output.println("Vendedor:");
            printBalanceChange(
                    result.getCurrency(),
                    result.getSellerBalanceBefore(),
                    result.getSellerBalanceAfter()
            );
            return;
        }

        output.println("Operación rechazada.");
        output.println("Motivo: " + purchaseRejectionMessage(result));
    }


    private void executeTransfer() {
        String rawRequestId = input.readOptionalText(
                "ID externo de solicitud UUID (vacío = generar): "
        );
        TransferRequestId requestId;
        try {
            requestId = rawRequestId.isBlank()
                    ? TransferRequestId.generate()
                    : TransferRequestId.parse(rawRequestId);
        } catch (IllegalArgumentException exception) {
            output.println("El identificador externo no tiene formato UUID válido.");
            return;
        }

        String sourceId = input.readRequiredText("ID del consumidor de origen: ");
        String destinationId = input.readRequiredText("ID del consumidor de destino: ");
        Currency currency = input.readEnumChoice("Moneda:", Currency.values());
        int quantity = input.readPositiveInt("Cantidad: ");
        String reference = input.readRequiredText("Concepto o referencia: ");

        if (!confirmExecution()) {
            output.println("Operación cancelada.");
            return;
        }

        TransferOperationResult result = transferService.transfer(
                new TransferRequest(
                        requestId,
                        sourceId,
                        destinationId,
                        currency,
                        quantity,
                        reference
                )
        );

        output.println("ID externo: " + result.getRequestId());
        output.println("ID de transacción: " + result.getTransactionId());

        if (result.isIdempotencyConflict()) {
            output.println("Operación rechazada por conflicto de idempotencia.");
            output.println(
                    "El mismo ID externo ya fue usado con datos diferentes."
            );
            return;
        }

        if (result.isIdempotentReplay()) {
            output.println("Respuesta idempotente: no se volvió a mover dinero.");
        }

        if (result.isCompleted()) {
            output.println("Transferencia completada.");
            printBalanceChange(
                    result.getCurrency(),
                    result.getSourceBalanceBefore().orElseThrow(),
                    result.getSourceBalanceAfter().orElseThrow()
            );
            output.println("Cuenta de destino:");
            printBalanceChange(
                    result.getCurrency(),
                    result.getDestinationBalanceBefore().orElseThrow(),
                    result.getDestinationBalanceAfter().orElseThrow()
            );
            return;
        }

        output.println("Transferencia rechazada.");
        output.println("Motivo: " + transferRejectionMessage(result));
    }

    private void showAccount() {
        String consumerId = input.readRequiredText("ID institucional, de persona o de cuenta: ");
        AccountSnapshot snapshot = accountQueryService.findAccount(consumerId)
                .orElse(null);

        if (snapshot == null) {
            output.println("Consumidor no encontrado.");
            return;
        }

        output.println(snapshot.getConsumerName() + " [" + snapshot.getInstitutionalAccountId() + "]");
        output.println("- ID estable de persona: " + snapshot.getStableConsumerId());
        output.println("- ID estable de cuenta: " + snapshot.getBankAccountId());
        output.println("- Profesión: " + snapshot.getProfession());
        output.println("- Posición censal: %05d".formatted(snapshot.getCensusPosition()));
        output.println("- Reutilización: " + snapshot.getReuseSequence());
        output.println("- Titularidad: " + snapshot.getHolderStatus());
        output.println("- Estado operativo: " + snapshot.getOperationalStatus());
        output.println("- Cambios de profesión completados: " + snapshot.getProfessionChangeCount());
        output.println("- Cambios de titularidad completados: " + snapshot.getHolderChangeCount());
        snapshot.getLastModifiedAt().ifPresent(value -> output.println("- Último cambio institucional: " + value));
        for (Currency currency : Currency.values()) {
            int balance = snapshot.getBalances().get(currency);
            output.println(
                    "- "
                            + currency
                            + ": "
                            + balance
                            + " "
                            + currency.getNameForQuantity(balance)
            );
        }
    }

    private void changeProfession() {
        String id = input.readRequiredText("ID institucional o de persona: ");
        String profession = input.readRequiredText("Nueva profesión canónica: ");
        ProfessionChangeResult result = professionChangeService.change(id, profession);
        if (!result.completed()) {
            output.println("Cambio rechazado: " + result.rejection().orElseThrow());
            output.println(result.message());
            return;
        }
        output.println("Cambio completado.");
        output.println("- Identificador anterior: " + result.previousInstitutionalId());
        output.println("- Identificador vigente: " + result.currentInstitutionalId());
        output.println("- Profesión anterior: " + result.previousProfession());
        output.println("- Profesión vigente: " + result.currentProfession());
    }

    private void releaseAccountHolder() {
        String id = input.readRequiredText("ID institucional, de persona o de cuenta: ");
        AccountReleaseResult result = accountHolderService.releaseHolder(id);
        output.println(result.completed() ? "Baja de titular completada." : "Baja de titular rechazada.");
        output.println(result.message());
    }


    private void manageAccountLifecycle() {
        String id = input.readRequiredText("ID institucional, de persona o de cuenta: ");
        AccountLifecycleAction action = input.readEnumChoice("Acción administrativa:", AccountLifecycleAction.values());
        AccountClosureReason reason = action == AccountLifecycleAction.CLOSE
                ? input.readEnumChoice("Motivo de cierre:", AccountClosureReason.values())
                : null;
        String external = input.readOptionalText("ID externo UUID (vacío = generar): ");
        AccountLifecycleRequestId requestId;
        try {
            requestId = external.isBlank() ? AccountLifecycleRequestId.generate() : AccountLifecycleRequestId.parse(external);
        } catch (IllegalArgumentException exception) {
            output.println("El identificador externo no tiene formato UUID válido.");
            return;
        }
        String reference = input.readRequiredText("Referencia administrativa: ");
        AccountLifecycleResult result = accountLifecycleService.process(
                new AccountLifecycleRequest(requestId, id, action, reason, reference)
        );
        output.println("ID externo: " + result.getRequestId());
        if (result.isIdempotentReplay()) output.println("Respuesta idempotente: no se repitió la transición.");
        if (result.isCompleted()) {
            output.println("Transición completada.");
            output.println("- Estado anterior: " + result.getPreviousStatus().orElseThrow());
            output.println("- Estado vigente: " + result.getCurrentStatus().orElseThrow());
        } else {
            output.println("Transición rechazada: " + result.getRejectionReason().orElseThrow());
        }
    }

    private void showAccountHistory() {
        String id = input.readRequiredText("ID institucional, de persona o de cuenta: ");
        AccountSnapshot snapshot = accountQueryService.findAccount(id).orElse(null);
        if (snapshot == null) {
            output.println("Cuenta no encontrada.");
            return;
        }
        var query = new AccountHistoryQuery(
                banking.identity.BankAccountId.parse(snapshot.getBankAccountId()), null, null, null,
                null, null, AccountHistorySortDirection.OLDEST_FIRST
        );
        var page = accountHistoryQueryService.search(query, new AccountHistoryPageRequest(0, 100));
        output.println("=== Historial longitudinal de " + snapshot.getInstitutionalAccountId() + " ===");
        if (page.content().isEmpty()) output.println("No hay eventos.");
        for (AccountHistoryEvent event : page.content()) {
            output.println(event.occurredAt() + " | " + event.type() + " | " + event.status());
            event.previousProfession().ifPresent(value -> output.println("  Profesión anterior: " + value));
            event.currentProfession().ifPresent(value -> output.println("  Profesión nueva/vigente: " + value));
            event.previousInstitutionalId().ifPresent(value -> output.println("  ID anterior: " + value));
            event.currentInstitutionalId().ifPresent(value -> output.println("  ID nuevo/vigente: " + value));
            event.previousOperationalStatus().ifPresent(value -> output.println("  Estado operativo anterior: " + value));
            event.currentOperationalStatus().ifPresent(value -> output.println("  Estado operativo vigente: " + value));
            event.closureReason().ifPresent(value -> output.println("  Motivo de cierre: " + value));
            event.rejectionReason().ifPresent(value -> output.println("  Rechazo: " + value));
        }
        AccountHistoryStatistics statistics = accountHistoryStatisticsService.calculate(query);
        output.println("Eventos: " + statistics.totalEvents()
                + " | completados: " + statistics.completedEvents()
                + " | rechazados: " + statistics.rejectedEvents());
        output.println("Cambios de profesión: " + statistics.professionChanges()
                + " | bajas/asignaciones de titular: "
                + (statistics.holderReleases() + statistics.holderAssignments()));
        statistics.averageProfessionChangeInterval().ifPresent(value ->
                output.println("Intervalo medio entre cambios de profesión: " + value));
    }

    private void showTransactionHistory() {
        boolean browsing = true;
        while (browsing) {
            output.println("=== Consulta operativa ===");
            output.println("1. Listar todas las operaciones");
            output.println("2. Buscar por identificador");
            output.println("3. Búsqueda avanzada");
            output.println("4. Estadísticas agregadas");
            output.println("0. Volver");

            int option = input.readInt("Selecciona una opción: ", 0, 4);
            output.println();

            switch (option) {
                case 1 -> browse(TransactionQuery.all(PageRequest.firstPage(10)));
                case 2 -> showTransactionById();
                case 3 -> advancedSearch();
                case 4 -> showStatistics(buildQuery(false));
                case 0 -> browsing = false;
                default -> throw new IllegalStateException("Unexpected option");
            }
            output.println();
        }
    }

    private void showTransactionById() {
        String value = input.readRequiredText("ID de transacción: ");
        TransactionId id;
        try {
            id = new TransactionId(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            output.println("El identificador no tiene un formato UUID válido.");
            return;
        }

        Optional<TransactionDetailView> detail =
                transactionQueryService.findById(id);
        if (detail.isEmpty()) {
            output.println("No existe una operación con ese identificador.");
            return;
        }
        printDetail(detail.orElseThrow());
    }

    private void advancedSearch() {
        TransactionQuery query = buildQuery(true);
        browse(query);
    }

    private TransactionQuery buildQuery(boolean includePagination) {
        Optional<TransactionType> type = readOptionalEnum(
                "Tipo de operación (0 = cualquiera):",
                TransactionType.values()
        );
        Optional<TransactionStatus> status = readOptionalEnum(
                "Estado (0 = cualquiera):",
                TransactionStatus.values()
        );
        String participant = input.readOptionalText(
                "ID de participante (vacío = cualquiera): "
        );
        Optional<Instant> from = readOptionalInstant(
                "Desde, inclusive, en ISO-8601 (vacío = sin límite): "
        );
        Optional<Instant> to = readOptionalInstant(
                "Hasta, exclusiva, en ISO-8601 (vacío = sin límite): "
        );
        SortDirection direction = input.readInt(
                "Orden: 1 = más reciente primero, 2 = más antiguo primero: ",
                1,
                2
        ) == 1 ? SortDirection.NEWEST_FIRST : SortDirection.OLDEST_FIRST;
        int pageSize = includePagination
                ? input.readInt("Tamaño de página (1-100): ", 1, 100)
                : 100;

        try {
            return new TransactionQuery(
                    type,
                    status,
                    participant.isBlank()
                            ? Optional.empty()
                            : Optional.of(participant),
                    from,
                    to,
                    direction,
                    PageRequest.firstPage(pageSize)
            );
        } catch (IllegalArgumentException exception) {
            output.println("Criterios inválidos: " + exception.getMessage());
            output.println("Vuelve a introducir los criterios de consulta.");
            return buildQuery(includePagination);
        }
    }

    private <E extends Enum<E>> Optional<E> readOptionalEnum(
            String title,
            E[] values
    ) {
        output.println(title);
        output.println("0. Cualquiera");
        for (int index = 0; index < values.length; index++) {
            output.println((index + 1) + ". " + values[index]);
        }
        int choice = input.readInt("Selecciona una opción: ", 0, values.length);
        return choice == 0
                ? Optional.empty()
                : Optional.of(values[choice - 1]);
    }

    private Optional<Instant> readOptionalInstant(String prompt) {
        while (true) {
            String value = input.readOptionalText(prompt);
            if (value.isBlank()) {
                return Optional.empty();
            }
            try {
                return Optional.of(Instant.parse(value));
            } catch (DateTimeParseException exception) {
                output.println(
                        "Fecha inválida. Ejemplo válido: 2026-07-19T10:15:30Z"
                );
            }
        }
    }

    private void browse(TransactionQuery initialQuery) {
        TransactionQuery currentQuery = initialQuery;
        boolean browsing = true;

        while (browsing) {
            TransactionPage<TransactionSummary> page =
                    transactionQueryService.search(currentQuery);
            printPage(page);

            output.println("1. Ver detalle por ID");
            if (page.hasPrevious()) {
                output.println("2. Página anterior");
            }
            if (page.hasNext()) {
                output.println("3. Página siguiente");
            }
            output.println("0. Volver");

            int option = input.readInt("Selecciona una opción: ", 0, 3);
            switch (option) {
                case 0 -> browsing = false;
                case 1 -> showTransactionById();
                case 2 -> {
                    if (page.hasPrevious()) {
                        currentQuery = withPage(
                                currentQuery,
                                currentQuery.pageRequest().pageNumber() - 1
                        );
                    } else {
                        output.println("No existe una página anterior.");
                    }
                }
                case 3 -> {
                    if (page.hasNext()) {
                        currentQuery = withPage(
                                currentQuery,
                                currentQuery.pageRequest().pageNumber() + 1
                        );
                    } else {
                        output.println("No existe una página siguiente.");
                    }
                }
                default -> throw new IllegalStateException("Unexpected option");
            }
        }
    }

    private TransactionQuery withPage(TransactionQuery query, int pageNumber) {
        return new TransactionQuery(
                query.type(),
                query.status(),
                query.participantId(),
                query.occurredFromInclusive(),
                query.occurredToExclusive(),
                query.sortDirection(),
                new PageRequest(pageNumber, query.pageRequest().pageSize())
        );
    }

    private void printPage(TransactionPage<TransactionSummary> page) {
        if (page.isEmpty()) {
            output.println("No hay operaciones que satisfagan los criterios.");
            return;
        }

        output.println("=== Resultados ===");
        for (TransactionSummary summary : page.content()) {
            output.println(
                    summary.occurredAt()
                            + " | " + summary.type()
                            + " | " + summary.status()
                            + " | " + summary.id()
            );
            output.println("  " + summary.description());
        }
        output.println(
                "Página " + (page.pageNumber() + 1)
                        + " de " + page.totalPages()
                        + " — " + page.totalElements() + " operaciones"
        );
    }

    private void printDetail(TransactionDetailView detail) {
        output.println("=== Detalle de operación ===");
        output.println("ID: " + detail.id());
        output.println("Fecha: " + detail.occurredAt());
        output.println("Tipo: " + detail.type());
        output.println("Estado: " + detail.status());
        output.println("Descripción: " + detail.description());
        if (!detail.participantIds().isEmpty()) {
            output.println("Participantes: " + String.join(", ", detail.participantIds()));
        }
        if (!detail.attributes().isEmpty()) {
            output.println("Atributos:");
            detail.attributes().forEach(
                    (name, value) -> output.println("- " + name + ": " + value)
            );
        }
    }

    private void showStatistics(TransactionQuery query) {
        TransactionStatistics statistics =
                transactionStatisticsService.calculate(query);
        output.println("=== Estadísticas agregadas ===");
        output.println("Total: " + statistics.total());
        output.println("Por tipo:");
        for (TransactionType type : TransactionType.values()) {
            output.println("- " + type + ": " + statistics.count(type));
        }
        output.println("Por estado:");
        for (TransactionStatus status : TransactionStatus.values()) {
            output.println("- " + status + ": " + statistics.count(status));
        }
    }

    private boolean confirmExecution() {
        output.println("1. Confirmar operación");
        output.println("0. Cancelar y volver al menú");
        return input.readInt("Selecciona una opción: ", 0, 1) == 1;
    }

    private void printBalanceChange(
            Currency currency,
            int before,
            int after
    ) {
        output.println(currency + ": " + before + " -> " + after);
    }

    private String mintRejectionMessage(MintOperationResult result) {
        MintOperationResult.RejectionReason reason = result
                .getRejectionReason()
                .orElseThrow();

        return switch (reason) {
            case INVALID_MATERIAL_COMPOSITION ->
                    "la composición declarada no corresponde al material.";
            case INSUFFICIENT_METAL ->
                    "no hay metal suficiente para una moneda.";
            case CONSUMER_NOT_FOUND -> "el consumidor no existe.";
            case MINT_POLICY_REJECTION -> switch (
                    result.getPolicyRejectionReason().orElseThrow()
            ) {
                case SPECIFICATION_NOT_FOUND ->
                        "no existe especificación de acuñación.";
                case MATERIAL_NOT_ALLOWED ->
                        "el material no está permitido para esta moneda.";
                case WEIGHT_NOT_ALLOWED ->
                        "el peso no está permitido para esta moneda.";
                case SEAL_NOT_ALLOWED ->
                        "el sello no corresponde a esta moneda.";
                case OPERATIONAL_LIMIT_EXCEEDED ->
                        "se ha superado un límite operativo.";
            };
        };
    }

    private String exchangeRejectionMessage(ExchangeOperationResult result) {
        ExchangeOperationResult.RejectionReason reason = result
                .getRejectionReason()
                .orElseThrow();

        if (reason == ExchangeOperationResult.RejectionReason.CONSUMER_NOT_FOUND) {
            return "el consumidor no existe.";
        }

        return switch (result.getPolicyRejectionReason().orElseThrow()) {
            case NON_POSITIVE_QUANTITY -> "la cantidad debe ser positiva.";
            case SAME_SOURCE_AND_TARGET_CURRENCY ->
                    "las monedas de origen y destino son iguales.";
            case EXCHANGE_ROUTE_NOT_ALLOWED ->
                    "la ruta de intercambio no está permitida.";
            case QUANTITY_NOT_EXACTLY_CONVERTIBLE ->
                    "la cantidad no admite conversión exacta.";
            case INSUFFICIENT_BALANCE -> "el saldo es insuficiente.";
            case ACCOUNT_NOT_OPERATIONAL -> "la cuenta no está operativa.";
            case OPERATIONAL_LIMIT_EXCEEDED -> "se ha superado un límite operativo.";
        };
    }

    private String transferRejectionMessage(TransferOperationResult result) {
        return switch (result.getRejectionReason().orElseThrow()) {
            case SOURCE_CONSUMER_NOT_FOUND -> "el consumidor de origen no existe.";
            case DESTINATION_CONSUMER_NOT_FOUND -> "el consumidor de destino no existe.";
            case SAME_SOURCE_AND_DESTINATION_ACCOUNT ->
                    "origen y destino pertenecen a la misma cuenta.";
            case NON_POSITIVE_QUANTITY -> "la cantidad debe ser positiva.";
            case INSUFFICIENT_BALANCE -> "el saldo de origen es insuficiente.";
            case SOURCE_ACCOUNT_NOT_OPERATIONAL -> "la cuenta de origen no está operativa.";
            case DESTINATION_ACCOUNT_NOT_OPERATIONAL -> "la cuenta de destino no está operativa.";
            case SOURCE_OPERATIONAL_LIMIT_EXCEEDED -> "la cuenta de origen ha superado un límite operativo.";
            case DESTINATION_OPERATIONAL_LIMIT_EXCEEDED -> "la cuenta de destino ha superado un límite operativo.";
        };
    }

    private String purchaseRejectionMessage(PurchaseOperationResult result) {
        PurchaseOperationResult.RejectionReason reason = result
                .getRejectionReason()
                .orElseThrow();

        return switch (reason) {
            case BUYER_NOT_FOUND -> "el comprador no existe.";
            case SELLER_NOT_FOUND -> "el vendedor no existe.";
            case CONSUMABLE_NOT_FOUND -> "el consumible no existe.";
            case TRANSACTION_POLICY_REJECTION -> switch (
                    result.getPolicyRejectionReason().orElseThrow()
            ) {
                case NON_POSITIVE_PRICE -> "el precio no es positivo.";
                case CURRENCY_NOT_ALLOWED_FOR_CONSUMABLE_TYPE ->
                        "la moneda no está permitida para este consumible.";
                case INSUFFICIENT_BUYER_BALANCE ->
                        "el comprador no tiene saldo suficiente.";
                case SAME_BUYER_AND_SELLER_ACCOUNT ->
                        "comprador y vendedor usan la misma cuenta.";
                case BUYER_ACCOUNT_NOT_OPERATIONAL ->
                        "la cuenta del comprador no está operativa.";
                case SELLER_ACCOUNT_NOT_OPERATIONAL ->
                        "la cuenta del vendedor no está operativa.";
                case BUYER_OPERATIONAL_LIMIT_EXCEEDED ->
                        "la cuenta del comprador ha superado un límite operativo.";
                case SELLER_OPERATIONAL_LIMIT_EXCEEDED ->
                        "la cuenta del vendedor ha superado un límite operativo.";
            };
        };
    }
}
