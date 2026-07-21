package tests;

import application.account.ProfessionChangeResult;
import application.account.ProfessionChangeService;
import banking.identity.PersonName;
import consumerRegistry.Consumer;
import consumerRegistry.ConsumerRegistry;

public final class BankIdentityAndCensusTest {
    public static void main(String[] args) {
        initialsAreDerivedCorrectly();
        exactKenanIdentityIsPreserved();
        professionChangeReusesLowestReleasedSlot();
        unsupportedProfessionIsRejectedWithoutMutation();
        System.out.println("BankIdentityAndCensusTest: OK");
    }

    private static void initialsAreDerivedCorrectly() {
        check(new PersonName("Álvaro").initials().equals("A"), "Álvaro -> A");
        check(new PersonName("María Luisa").initials().equals("ML"), "María Luisa -> ML");
        check(new PersonName("Juan-Pablo").initials().equals("JP"), "Juan-Pablo -> JP");
    }

    private static void exactKenanIdentityIsPreserved() {
        ConsumerRegistry registry = new ConsumerRegistry();
        Consumer kenan = registry.registerExact("Kenan", "Guerrero de Ébano", 25399, 0);
        check(kenan.getConsumerId().equals("K-GE-25399"), "Kenan institutional id");
        check(kenan.getStableConsumerId() != null, "stable consumer id");
        check(kenan.getBankAccount().getBankAccountId() != null, "stable account id");
    }

    private static void professionChangeReusesLowestReleasedSlot() {
        ConsumerRegistry registry = new ConsumerRegistry();
        Consumer first = registry.register("Álvaro", "Carpintero");
        Consumer second = registry.register("María Luisa", "Carpintero");
        ProfessionChangeService service = new ProfessionChangeService(registry);
        ProfessionChangeResult moved = service.change(first.getConsumerId(), "Jornalero");
        check(moved.completed(), "profession change completes");
        Consumer third = registry.register("Juan-Pablo", "Carpintero");
        check(third.getBankAccount().getCensusPosition().value() == 1, "lowest released slot reused");
        check(third.getBankAccount().getReuseSequence().value() == 1, "reuse sequence increments");
        check(third.getConsumerId().equals("JP-Car-00001-1"), "reused institutional id");
        check(second.getConsumerId().equals("ML-Car-00002"), "other account unchanged");
    }

    private static void unsupportedProfessionIsRejectedWithoutMutation() {
        ConsumerRegistry registry = new ConsumerRegistry();
        Consumer person = registry.register("Álvaro", "Carpintero");
        String before = person.getConsumerId();
        ProfessionChangeResult result = new ProfessionChangeService(registry).change(before, "Alquimista");
        check(!result.completed(), "unsupported profession rejected");
        check(person.getConsumerId().equals(before), "identity remains unchanged");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
