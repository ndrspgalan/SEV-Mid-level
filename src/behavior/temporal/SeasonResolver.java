package behavior.temporal;

import java.time.Instant;

public interface SeasonResolver {
    SeasonPeriod resolve(Instant instant);
}
