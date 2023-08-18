/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
