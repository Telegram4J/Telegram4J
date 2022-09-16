package telegram4j.core.event.domain.inline;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.User;
import telegram4j.core.object.markup.KeyboardButton;
import telegram4j.core.spec.AnswerCallbackQuerySpec;
import telegram4j.tl.request.messages.SetBotCallbackAnswer;

import java.util.Optional;

/** Subtype of callback events invoking on pushing {@link KeyboardButton} with type {@link KeyboardButton.Type#CALLBACK}. */
public class CallbackEvent extends BotEvent {
    private final long queryId;
    private final User user;
    private final long chatInstance;
    @Nullable
    private final ByteBuf data;
    @Nullable
    private final String gameShortName;

    public CallbackEvent(MTProtoTelegramClient client, long queryId, User user,
                         long chatInstance, @Nullable ByteBuf data,
                         @Nullable String gameShortName) {
        super(client);
        this.queryId = queryId;
        this.user = user;
        this.chatInstance = chatInstance;
        this.data = data;
        this.gameShortName = gameShortName;
    }

    @Override
    public long getQueryId() {
        return queryId;
    }

    @Override
    public User getUser() {
        return user;
    }

    /**
     * Gets global identifier which uniquely corresponding to chat and message where callback was invoked.
     * Useful for high scored in games.
     *
     * @return The global identifier which uniquely corresponding to chat and message where callback was invoked.
     */
    public long getChatInstance() {
        return chatInstance;
    }

    /**
     * Gets callback data in {@link ByteBuf}, if it's not a game callback.
     *
     * @return The {@link ByteBuf} with callback data, if it's not a game callback.
     */
    public Optional<ByteBuf> getData() {
        return Optional.ofNullable(data).map(ByteBuf::duplicate);
    }

    /**
     * Gets short name of game, if present.
     *
     * @return The short name of game, if present.
     */
    public Optional<String> getGameShortName() {
        return Optional.ofNullable(gameShortName);
    }

    /**
     * Request to set answer for callback by given specification.
     *
     * @param spec The specification for callback answer.
     * @return A {@link Mono} which emitting on completion boolean which displays completion state.
     */
    public Mono<Boolean> answer(AnswerCallbackQuerySpec spec) {
        return Mono.defer(() -> client.getServiceHolder().getChatService()
                .setBotCallbackAnswer(SetBotCallbackAnswer.builder()
                        .queryId(getQueryId())
                        .alert(spec.alert())
                        .message(spec.message().orElse(null))
                        .url(spec.url().orElse(null))
                        .cacheTime(Math.toIntExact(spec.cacheTime().getSeconds()))
                        .build()));
    }

    @Override
    public String toString() {
        return "CallbackEvent{" +
                "queryId=" + queryId +
                ", user=" + user +
                ", chatInstance=" + chatInstance +
                ", data=" + data +
                ", gameShortName='" + gameShortName + '\'' +
                '}';
    }
}
