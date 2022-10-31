package telegram4j.core.object.chat;

import telegram4j.core.util.Id;

import java.util.Objects;

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

    public Id getPeerId() {
        return peerId;
    }
}
