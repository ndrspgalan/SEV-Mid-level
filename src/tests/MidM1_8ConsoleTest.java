package tests;

import application.ValerianEconomicSystem;
import application.ValerianEconomicSystemBootstrap;
import console.MidConsoleFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class MidM1_8ConsoleTest {
    private MidM1_8ConsoleTest() {}

    public static void main(String[] args) {
        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();
        String script = String.join("\n", "2", "1", "5", "0", "0") + "\n";
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        MidConsoleFactory.create(system,
                new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(buffer, true, StandardCharsets.UTF_8)).run();
        String output = buffer.toString(StandardCharsets.UTF_8);
        require(output.contains("Nivel Mid (M1)"), "mid title missing");
        require(output.contains("Proyección finalizada."), "projection output missing");
        require(output.contains("Auditoría: VÁLIDA"), "audit must be valid after projection");
        require(system.getEconomicEventRepository().count() > 0, "projection must create events");
        System.out.println("MidM1_8ConsoleTest: PASSED");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
