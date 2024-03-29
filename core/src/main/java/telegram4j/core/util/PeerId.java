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
package telegram4j.core.util;

import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;

/**
 * Identifier that's contains the {@link Id id} or {@link String username} of a peer entity.
 * For now, used in specs and resolve methods.
 */
public final class PeerId {
    @Nullable
    private final String username;
    @Nullable
    private final Id id;

    private PeerId(String username) {
        this.username = Objects.requireNonNull(username);
        this.id = null;
    }

    private PeerId(Id id) {
        this.id = Objects.requireNonNull(id);
        this.username = null;
    }

    /**
     * Constructs a {@code PeerId} from given {@link Id} of a peer entity.
     *
     * @param id The id of a peer entity.
     * @return The new {@code PeerId} from given id.
     */
    public static PeerId of(Id id) {
        return new PeerId(id);
    }

    /**
     * Constructs a {@code PeerId} from given peer entity's username.
     *
     * @param username The username of a peer entity.
     * @return The new {@code PeerId} from given username.
     */
    public static PeerId of(String username) {
        if (username.startsWith("@")) {
            username = username.substring(1);
        }
        return new PeerId(username);
    }

    /**
     * Gets a username value variant, if present.
     *
     * @return The peer entity's username, if present.
     */
    public Optional<String> asUsername() {
        return Optional.ofNullable(username);
    }

    /**
     * Gets a id value variant, if present.
     *
     * @return The id of peer entity, if present.
     */
    public Optional<Id> asId() {
        return Optional.ofNullable(id);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerId peerId = (PeerId) o;
        return Objects.equals(username, peerId.username) && Objects.equals(id, peerId.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(username) + Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "PeerId{" + (username != null ? username : id) + '}';
    }
}
