package telegram4j.core.object.chat;

import telegram4j.core.util.Id;

import java.util.Objects;

public class UnresolvedPeerException extends RuntimeException {
    private final Id peerId;

    public UnresolvedPeerException(Id peerId) {
        super("No information about the " + peerId + " peer present in local storage");
        this.peerId = Objects.requireNonNull(peerId);
    }

    public Id getPeerId() {
        return peerId;
    }
}
