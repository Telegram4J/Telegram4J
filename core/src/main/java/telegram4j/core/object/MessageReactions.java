package telegram4j.core.object;

import telegram4j.core.MTProtoTelegramClient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Message reactions information. */
public class MessageReactions implements TelegramObject {
    private final MTProtoTelegramClient client;
    private final telegram4j.tl.MessageReactions data;

    public MessageReactions(MTProtoTelegramClient client, telegram4j.tl.MessageReactions data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets whether this is minimal information about reactions.
     *
     * @return {@code true} if it is minimal information about reactions.
     */
    public boolean isMin() {
        return data.min();
    }

    /**
     * Gets whether <i>current</i> user can see detailed list of peers which reacted to the message.
     *
     * @return {@code true} if <i>current</i> user can see detailed list of peers which reacted to the message.
     */
    public boolean isCanSeeList() {
        return data.canSeeList();
    }

    /**
     * Gets list of count of reactions.
     *
     * @return The mutable {@link List} of count of reactions.
     */
    public List<ReactionCount> getResults() {
        return data.results().stream()
                .map(ReactionCount::new)
                .collect(Collectors.toList());
    }

    /**
     * Gets list of recent peers and their reactions.
     *
     * @return The mutable {@link List} of recent peers and their reactions.
     */
    public Optional<List<MessagePeerReaction>> getRecentReactions() {
        return Optional.ofNullable(data.recentReactions())
                .map(list -> list.stream()
                        .map(d -> new MessagePeerReaction(client, d))
                        .collect(Collectors.toList()));
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public String toString() {
        return "MessageReactions{" +
                "data=" + data +
                '}';
    }
}
