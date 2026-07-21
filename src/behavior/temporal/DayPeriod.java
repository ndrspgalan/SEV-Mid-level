package behavior.temporal;

import java.time.LocalTime;

public enum DayPeriod {
    NIGHT, MORNING, AFTERNOON;
    public static DayPeriod from(LocalTime time) {
        int block = time.getHour() / 8;
        return switch (block) { case 0 -> NIGHT; case 1 -> MORNING; default -> AFTERNOON; };
    }
}
