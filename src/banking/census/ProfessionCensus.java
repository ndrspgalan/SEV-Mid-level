package banking.census;

import banking.identity.BankAccountId;
import banking.identity.CensusPosition;
import banking.identity.Profession;
import banking.identity.ReuseSequence;

import java.util.*;

public final class ProfessionCensus {
    private final Map<Profession, List<ProfessionCensusSlot>> slots = new HashMap<>();

    public synchronized ProfessionCensusSlot allocate(Profession profession, BankAccountId accountId) {
        Objects.requireNonNull(profession); Objects.requireNonNull(accountId);
        List<ProfessionCensusSlot> professionSlots = slots.computeIfAbsent(profession, ignored -> new ArrayList<>());
        ProfessionCensusSlot reusable = professionSlots.stream()
                .filter(slot -> slot.status() == CensusSlotStatus.AVAILABLE)
                .min(Comparator.comparing(ProfessionCensusSlot::position))
                .orElse(null);
        if (reusable != null) { reusable.occupy(accountId); return reusable; }
        int next = professionSlots.stream().mapToInt(slot -> slot.position().value()).max().orElse(0) + 1;
        if (next > CensusPosition.MAX) throw new IllegalStateException("profession census is saturated: " + profession.name());
        ProfessionCensusSlot created = new ProfessionCensusSlot(profession, new CensusPosition(next), new ReuseSequence(0), accountId);
        professionSlots.add(created);
        return created;
    }

    public synchronized ProfessionCensusSlot reserveExact(Profession profession, CensusPosition position, ReuseSequence reuse, BankAccountId accountId) {
        List<ProfessionCensusSlot> professionSlots = slots.computeIfAbsent(profession, ignored -> new ArrayList<>());
        boolean exists = professionSlots.stream().anyMatch(slot -> slot.position().equals(position));
        if (exists) throw new IllegalArgumentException("census position already exists");
        ProfessionCensusSlot slot = new ProfessionCensusSlot(profession, position, reuse, accountId);
        professionSlots.add(slot);
        return slot;
    }

    public synchronized void release(Profession profession, CensusPosition position, BankAccountId accountId) {
        ProfessionCensusSlot slot = findSlot(profession, position);
        if (!slot.accountId().orElseThrow().equals(accountId)) throw new IllegalStateException("slot belongs to another account");
        slot.release();
    }

    public synchronized List<ProfessionCensusSlot> findByProfession(Profession profession) {
        return List.copyOf(slots.getOrDefault(profession, List.of()));
    }

    private ProfessionCensusSlot findSlot(Profession profession, CensusPosition position) {
        return slots.getOrDefault(profession, List.of()).stream()
                .filter(slot -> slot.position().equals(position)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("census slot not found"));
    }
}
