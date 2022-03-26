package telegram4j.core.object;

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
        this.username = Objects.requireNonNull(username, "username");
        this.id = null;
    }

    private PeerId(Id id) {
        this.id = Objects.requireNonNull(id, "id");
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
     * @throws IllegalArgumentException If username doesn't start from {@literal @} char.
     * @param username The username of a peer entity.
     * @return The new {@code PeerId} from given username.
     */
    public static PeerId of(String username) {
        if (!username.startsWith("@")) {
            throw new IllegalArgumentException("Malformed peer id: '" + username + "'");
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
        return Objects.hash(username, id);
    }

    @Override
    public String toString() {
        return "PeerId{" + (username != null ? username : id) + '}';
    }
}
