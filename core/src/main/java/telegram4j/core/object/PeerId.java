package telegram4j.core.object;

import reactor.util.annotation.Nullable;

import java.util.Objects;
import java.util.Optional;

public final class PeerId {
    @Nullable
    private final String username;
    @Nullable
    private final Id id;

    PeerId(String username) {
        this.username = Objects.requireNonNull(username, "username");
        this.id = null;
    }

    PeerId(Id id) {
        this.id = Objects.requireNonNull(id, "id");
        this.username = null;
    }

    public static PeerId of(Id id) {
        return new PeerId(id);
    }

    public static PeerId of(String username) {
        if (!username.startsWith("@")) {
            throw new IllegalArgumentException("Malformed peer id.");
        }
        return new PeerId(username);
    }

    public Optional<String> asUsername() {
        return Optional.ofNullable(username);
    }

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
