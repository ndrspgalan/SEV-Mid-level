package banking.census;

import banking.identity.Profession;
import banking.identity.ProfessionCode;

import java.util.*;

public final class ProfessionCatalog {
    private final Map<String, Profession> byName = new LinkedHashMap<>();
    public ProfessionCatalog register(String name, String code) {
        Profession profession = new Profession(name, ProfessionCode.of(code));
        String key = normalize(name);
        if (byName.putIfAbsent(key, profession) != null) throw new IllegalArgumentException("profession already registered: " + name);
        return this;
    }
    public ProfessionCatalog registerAlias(String alias, String canonicalName) {
        Profession profession = require(canonicalName);
        String key = normalize(alias);
        if (byName.putIfAbsent(key, profession) != null) throw new IllegalArgumentException("profession alias already registered: " + alias);
        return this;
    }
    public Optional<Profession> find(String name) { return Optional.ofNullable(byName.get(normalize(name))); }
    public Profession require(String name) { return find(name).orElseThrow(() -> new IllegalArgumentException("profession not accepted by bank: " + name)); }
    public Collection<Profession> all() { return ListCopy.copy(byName.values()); }
    private static String normalize(String value) { Objects.requireNonNull(value); return value.trim().toLowerCase(Locale.ROOT); }
    private static final class ListCopy { static <T> Collection<T> copy(Collection<T> source) { return List.copyOf(source); } }
    public static ProfessionCatalog valerianStandard() {
        return new ProfessionCatalog()
                .register("Guerrero de Ébano", "GE")
                .register("Comerciante", "Co")
                .register("Cortesana", "Cor")
                .register("Mercenario", "Mer")
                .register("Mendigo", "Men")
                .register("Noble", "No")
                .register("Soldado", "Sol")
                .register("Herrero", "Her")
                .register("Carpintero", "Car")
                .register("Feriante", "Fer")
                .register("Maestro", "Mae")
                .register("Jurista", "Jur")
                .register("Cazador", "Caz")
                .register("Marinero", "Mar")
                .register("Curtidor", "Cur")
                .register("Modista", "Mod")
                .register("Peluquero", "Pel")
                .register("Cantero", "Can")
                .register("Jornalero", "Jor")
                .registerAlias("Mercader", "Comerciante")
                .registerAlias("Trader", "Comerciante")
                .registerAlias("Merchant", "Comerciante");
    }
}
