package console;

import application.behavior.BehaviorProfileService;
import banking.identity.ConsumerId;
import behavior.profile.BehaviorProfile;
import behavior.profile.ConsumableBehaviorProfile;

import java.io.PrintStream;
import java.util.Objects;

/** Minimal M2.2 console for rebuilding and inspecting descriptive behavioral profiles. */
public final class BehaviorProfileConsole {
    private final ConsoleInput input;
    private final PrintStream output;
    private final BehaviorProfileService service;

    public BehaviorProfileConsole(ConsoleInput input, PrintStream output, BehaviorProfileService service) {
        this.input = Objects.requireNonNull(input);
        this.output = Objects.requireNonNull(output);
        this.service = Objects.requireNonNull(service);
    }

    public void run() {
        boolean running = true;
        while (running) {
            output.println("=== Análisis conductual — M2.2 ===");
            output.println("1. Regenerar perfiles conductuales");
            output.println("2. Listar perfiles");
            output.println("3. Consultar perfil por titular");
            output.println("0. Volver");
            int option = input.readInt("Selecciona una opción: ", 0, 3);
            output.println();
            switch (option) {
                case 1 -> output.println("Perfiles generados: " + service.rebuildProfiles().size());
                case 2 -> list();
                case 3 -> find();
                case 0 -> running = false;
                default -> throw new IllegalStateException("Unexpected option");
            }
            output.println();
        }
    }

    private void list() {
        if (service.findAll().isEmpty()) {
            output.println("No hay perfiles generados.");
            return;
        }
        for (BehaviorProfile profile : service.findAll()) {
            output.println(profile.consumerId() + " | eventos=" + profile.totalEvents()
                    + " | rechazados=" + profile.rejectedEvents()
                    + " | consumibles=" + profile.consumables().size());
        }
    }

    private void find() {
        String raw = input.readRequiredText("Identificador estable del titular: ");
        try {
            service.findByConsumerId(ConsumerId.parse(raw)).ifPresentOrElse(
                    this::print,
                    () -> output.println("No existe un perfil para ese titular."));
        } catch (IllegalArgumentException ex) {
            output.println("Identificador de titular inválido.");
        }
    }

    private void print(BehaviorProfile profile) {
        output.println("Titular: " + profile.consumerId());
        output.println("Periodo observado: " + profile.firstEventAt() + " -> " + profile.lastEventAt());
        output.println("Eventos: " + profile.totalEvents() + " (correctos=" + profile.succeededEvents()
                + ", rechazados=" + profile.rejectedEvents() + ")");
        output.println("Contrapartes conocidas: " + profile.counterparties().size());
        output.println("Volumen por moneda: " + profile.succeededVolumeByCurrency());
        output.println("Consumibles:");
        if (profile.consumables().isEmpty()) output.println("  Sin compras completadas.");
        for (ConsumableBehaviorProfile value : profile.consumables().values()) {
            output.println("  " + value.consumableName() + " | compras=" + value.purchaseCount()
                    + " | unidades=" + value.unitsPurchased() + " | gasto=" + value.totalSpentByCurrency());
        }
    }
}
