package telegram4j.core.event.domain.message;

import io.netty.buffer.ByteBuf;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.User;
import telegram4j.core.object.media.Poll.Flag;

import java.util.List;
import java.util.stream.Collectors;

/** Event of a new vote in the poll. */
public class MessagePollVoteEvent extends MessageEvent {
    private final long pollId;
    private final User user;
    private final List<ByteBuf> options;

    public MessagePollVoteEvent(MTProtoTelegramClient client, long pollId, User user, List<ByteBuf> options) {
        super(client);
        this.pollId = pollId;
        this.user = user;
        this.options = options;
    }

    /**
     * Gets id of poll.
     *
     * @return The id of poll.
     */
    public long getPollId() {
        return pollId;
    }

    /**
     * Gets voted user.
     *
     * @return The voted user.
     */
    public User getUser() {
        return user;
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
                "pollId=" + pollId +
                ", user=" + user +
                ", options=" + options +
                '}';
    }
}
