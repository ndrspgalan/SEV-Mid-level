import application.ValerianEconomicSystem;
import application.ValerianEconomicSystemBootstrap;
import console.MidConsoleFactory;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        ValerianEconomicSystem system = ValerianEconomicSystemBootstrap.createJuniorSystem();
        MidConsoleFactory.create(system, System.in, System.out).run();
    }
}
