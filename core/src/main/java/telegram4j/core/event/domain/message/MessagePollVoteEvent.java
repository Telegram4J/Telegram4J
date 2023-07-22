package telegram4j.core.event.domain.message;

import io.netty.buffer.ByteBuf;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.media.Poll;
import telegram4j.core.object.media.Poll.Flag;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Event of a new vote in the poll. */
public final class MessagePollVoteEvent extends MessageEvent {
    @Nullable
    private final Poll poll;
    private final PeerEntity peer;
    private final List<ByteBuf> options;

    public MessagePollVoteEvent(MTProtoTelegramClient client, @Nullable Poll poll, PeerEntity peer, List<ByteBuf> options) {
        super(client);
        this.poll = poll;
        this.peer = peer;
        this.options = options;
    }

    /**
     * Gets poll object, if present.
     *
     * @return The poll object, if present.
     */
    public Optional<Poll> getPoll() {
        return Optional.ofNullable(poll);
    }

    /**
     * Gets voted peer.
     *
     * @return The voted peer.
     */
    public PeerEntity getPeer() {
        return peer;
    }

    /**
     * Gets the options selected by the user.
     * Can contain more than one option if poll have {@link Flag#MULTIPLE_CHOICE} flag.
     *
     * @return The list of selected options.
     */
    public List<ByteBuf> getOptions() {
        return options.stream()
                .map(ByteBuf::duplicate)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "MessagePollVoteEvent{" +
                "poll=" + poll +
                ", peer=" + peer +
                ", options=" + options +
                '}';
    }
}
