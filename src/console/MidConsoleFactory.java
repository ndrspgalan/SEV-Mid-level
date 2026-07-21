package console;

import application.ValerianEconomicSystem;
import application.analytics.EconomicEventQueryService;
import application.analytics.EconomicEventStatisticsService;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Objects;

public final class MidConsoleFactory {
    private MidConsoleFactory() {}

    public static MidConsole create(ValerianEconomicSystem system, InputStream input, PrintStream output) {
        Objects.requireNonNull(system);
        Objects.requireNonNull(input);
        Objects.requireNonNull(output);
        ConsoleInput consoleInput = new ConsoleInput(input, output);
        EconomicEventQueryService queryService = new EconomicEventQueryService(system.getEconomicEventRepository());
        EconomicEventConsole analytics = new EconomicEventConsole(
                consoleInput,
                output,
                system.getEconomicEventProjectionService(),
                queryService,
                new EconomicEventStatisticsService(queryService),
                system.getEconomicEventInvariantAuditor());
        BehaviorProfileConsole behavior = new BehaviorProfileConsole(
                consoleInput, output, system.getBehaviorProfileService());
        return new MidConsole(consoleInput, output,
                JuniorConsoleFactory.create(system, consoleInput, output), analytics, behavior);
    }
}
