package telegram4j.mtproto.client;

import java.time.Duration;
import java.util.Optional;

public interface ReconnectionContext {

    /** {@return current attempt of reconnecting} One-based value */
    int iteration();

    /** {@return previous backoff time} Null when it's first attempt */
    Optional<Duration> lastBackoff();

    /** {@return cause of reconnecting} If there is a reason */
    Optional<Throwable> exception();
}