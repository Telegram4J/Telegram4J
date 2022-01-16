package telegram4j.mtproto.store;

import reactor.util.annotation.Nullable;
import telegram4j.tl.InputPeer;
import telegram4j.tl.Message;

import java.util.List;
import java.util.Objects;

public class ResolvedDeletedMessages {

    private final InputPeer peer;
    private final List<Message> messages;

    public ResolvedDeletedMessages(InputPeer peer, List<Message> messages) {
        this.peer = Objects.requireNonNull(peer, "peer");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    public InputPeer getPeer() {
        return peer;
    }

    public List<Message> getMessages() {
        return messages;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolvedDeletedMessages that = (ResolvedDeletedMessages) o;
        return peer.equals(that.peer) && messages.equals(that.messages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(peer, messages);
    }

    @Override
    public String toString() {
        return "ResolvedDeletedMessages{" +
                "peer=" + peer +
                ", messages=" + messages +
                '}';
    }
}
