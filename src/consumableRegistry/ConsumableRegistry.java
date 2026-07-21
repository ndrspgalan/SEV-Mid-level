package consumableRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class ConsumableRegistry {

    private final Map<String, Consumable> consumables =
            new HashMap<>();

    public Consumable register(Consumable consumable) {
        Objects.requireNonNull(consumable, "consumable must not be null");

        String consumableId = consumable.getConsumableId();
        if (consumables.containsKey(consumableId)) {
            throw new IllegalArgumentException(
                    "Consumable with id "
                            + consumableId
                            + " already exists"
            );
        }

        consumables.put(consumableId, consumable);
        return consumable;
    }

    public Optional<Consumable> findById(String consumableId) {
        String normalizedConsumableId = normalizeRequiredId(
                consumableId,
                "consumableId"
        );
        return Optional.ofNullable(consumables.get(normalizedConsumableId));
    }

    private static String normalizeRequiredId(
            String value,
            String fieldName
    ) {
        Objects.requireNonNull(value, fieldName + " must not be null");

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }
        return normalizedValue;
    }
}
