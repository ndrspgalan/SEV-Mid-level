package console;

import java.io.PrintStream;
import java.util.Objects;

/** Entry console that preserves Junior operations and exposes the M1 analytical subsystem. */
public final class MidConsole {
    private final ConsoleInput input;
    private final PrintStream output;
    private final JuniorConsole juniorConsole;
    private final EconomicEventConsole economicEventConsole;
    private final BehaviorProfileConsole behaviorProfileConsole;

    public MidConsole(ConsoleInput input, PrintStream output, JuniorConsole juniorConsole,
                      EconomicEventConsole economicEventConsole,
                      BehaviorProfileConsole behaviorProfileConsole) {
        this.input = Objects.requireNonNull(input);
        this.output = Objects.requireNonNull(output);
        this.juniorConsole = Objects.requireNonNull(juniorConsole);
        this.economicEventConsole = Objects.requireNonNull(economicEventConsole);
        this.behaviorProfileConsole = Objects.requireNonNull(behaviorProfileConsole);
    }

    public void run() {
        output.println("=== Sistema Económico Valeriano — Nivel Mid (M1) + M2.2 ===");
        boolean running = true;
        while (running) {
            output.println("1. Operaciones económicas Junior");
            output.println("2. Análisis de eventos económicos");
            output.println("3. Análisis conductual agregado");
            output.println("0. Salir");
            int option = input.readInt("Selecciona una opción: ", 0, 3);
            output.println();
            switch (option) {
                case 1 -> juniorConsole.run();
                case 2 -> economicEventConsole.run();
                case 3 -> behaviorProfileConsole.run();
                case 0 -> running = false;
                default -> throw new IllegalStateException("Unexpected option");
            }
            output.println();
        }
        output.println("Aplicación finalizada.");
    }
}
