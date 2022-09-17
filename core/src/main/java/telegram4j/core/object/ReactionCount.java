package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.EntityFactory;

import java.util.Objects;
import java.util.Optional;

public class ReactionCount implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.ReactionCount data;

    public ReactionCount(MTProtoTelegramClient client, telegram4j.tl.ReactionCount data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public Optional<Integer> getChosenOrder() {
        return Optional.ofNullable(data.chosenOrder());
    }

    public Reaction getReaction() {
        return EntityFactory.createReaction(data.reaction());
    }

    public int getCount() {
        return data.count();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReactionCount that = (ReactionCount) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ReactionCount{" +
                "data=" + data +
                '}';
    }
}
