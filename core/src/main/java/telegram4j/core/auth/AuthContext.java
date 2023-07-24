package telegram4j.core.auth;

import java.util.Objects;

/**
 * Base subtype of the auth context.
 * There is no base logic, just an ID for tracking the auth flow in logs.
 */
public abstract class AuthContext {
    /** An ID of current auth flow. It's hex string of identity hash code value. */
    protected final String id;

    /** Creates new {@code AuthContext} with new ID. */
    protected AuthContext() {
        this.id = Integer.toHexString(System.identityHashCode(this));
    }

    /**
     * Creates new {@code AuthContext} with specified auth flow ID.
     * It is useful when authorization is divided into stages, such as 2FA.
     *
     * @param id The ID of auth flow.
     */
    protected AuthContext(String id) {
        this.id = Objects.requireNonNull(id);
    }

    /**
     * Logs specified message to internal logger with custom formatting.
     *
     * @param message The message to log.
     */
    public void log(String message) {
        log0("[I:0x" + id + "] " + message);
    }

    /**
     * Actual implementation of {@link #log(String)}
     *
     * @param message The log message with applied formatting.
     */
    protected abstract void log0(String message);
}
