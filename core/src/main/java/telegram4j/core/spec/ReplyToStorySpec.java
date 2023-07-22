package telegram4j.core.spec;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.util.Id;
import telegram4j.tl.ImmutableInputReplyToStory;
import telegram4j.tl.InputReplyTo;

import java.util.Objects;

public final class ReplyToStorySpec implements ReplySpec {
    private final Id userId;
    private final int storyId;

    private ReplyToStorySpec(Id userId, int storyId) {
        this.userId = userId;
        this.storyId = storyId;
    }

    public Id userId() {
        return userId;
    }

    public int storyId() {
        return storyId;
    }

    public static ReplyToStorySpec of(Id userId, int storyId) {
        Objects.requireNonNull(userId);
        return new ReplyToStorySpec(userId, storyId);
    }

    @Override
    public Mono<ImmutableInputReplyToStory> resolve(MTProtoTelegramClient client) {
        return client.asInputUserExact(userId)
                .map(inputUser -> ImmutableInputReplyToStory.of(inputUser, storyId));

    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplyToStorySpec that)) return false;
        return storyId == that.storyId && userId.equals(that.userId);
    }

    @Override
    public int hashCode() {
        int h = 5381;
        h += (h << 5) + userId.hashCode();
        h += (h << 5) + storyId;
        return h;
    }

    @Override
    public String toString() {
        return "ReplyToStorySpec{" +
                "userId=" + userId +
                ", storyId=" + storyId +
                '}';
    }
}
