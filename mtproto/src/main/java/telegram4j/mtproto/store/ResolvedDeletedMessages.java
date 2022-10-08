package telegram4j.mtproto.store;

import telegram4j.tl.BaseMessageFields;
import telegram4j.tl.InputPeer;

import java.util.List;
import java.util.Objects;

/** Container object with found deleted messages and peer id. */
public class ResolvedDeletedMessages {

    private final InputPeer peer;
    private final List<BaseMessageFields> messages;

    public ResolvedDeletedMessages(InputPeer peer, List<BaseMessageFields> messages) {
        this.peer = Objects.requireNonNull(peer);
        this.messages = Objects.requireNonNull(messages);
    }

    public InputPeer getPeer() {
        return peer;
    }

    public List<BaseMessageFields> getMessages() {
        return messages;
    }

    @Override
    public String toString() {
        return "ResolvedDeletedMessages{" +
                "peer=" + peer +
                ", messages=" + messages +
                '}';
    }
}
