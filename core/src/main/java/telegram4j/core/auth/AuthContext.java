package telegram4j.core.auth;

import java.util.Objects;

public abstract class AuthContext {
    protected final String id;

    protected AuthContext() {
        this.id = Integer.toHexString(System.identityHashCode(this));
    }

    protected AuthContext(String id) {
        this.id = Objects.requireNonNull(id);
    }

    public void logInfo(String message) {
        logInfo0("[I:0x" + id + "] " + message);
    }

    protected abstract void logInfo0(String message);
}
