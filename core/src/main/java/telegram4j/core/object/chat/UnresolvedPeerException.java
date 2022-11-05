package telegram4j.core.object.chat;

import telegram4j.core.util.Id;
import telegram4j.tl.InputPeer;

import java.util.Objects;

/** Unchecked exception used to correctly process resolving of {@link InputPeer} object. */
public class UnresolvedPeerException extends RuntimeException {
    private final Id peerId;

    public UnresolvedPeerException(Id peerId) {
        super("Have no access to " + peerId + " peer" + peerId.getMinInformation()
                .map(Id.MinInformation::toString)
                .or(() -> peerId.getAccessHash().map(Object::toString))
                .map(s -> " with access info: " + s)
                .orElse("") +
                " or it's not present in local storage");
        this.peerId = Objects.requireNonNull(peerId);
    }

    /**
     * Gets id of peer which couldn't resolve in local storage,
     *
     * @return The id of peer which couldn't resolve in local storage,
     */
    public Id getPeerId() {
        return peerId;
    }
}
