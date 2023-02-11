package telegram4j.mtproto.store.object;

import telegram4j.tl.InputPeer;
import telegram4j.tl.Message;

import java.util.List;
import java.util.Objects;

/**
 * Container object with found deleted messages and
 * peer id where messages was deleted.
 */
public record ResolvedDeletedMessages(InputPeer peer, List<Message> messages) {

    public ResolvedDeletedMessages {
        Objects.requireNonNull(peer);
        Objects.requireNonNull(messages);
    }
}
