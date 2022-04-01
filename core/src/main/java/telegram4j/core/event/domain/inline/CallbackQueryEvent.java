package telegram4j.core.event.domain.inline;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;
import telegram4j.core.object.User;
import telegram4j.core.spec.AnswerCallbackQuerySpec;
import telegram4j.tl.request.messages.SetBotCallbackAnswer;

public class CallbackQueryEvent extends CallbackEvent {
    private final Id peerId;
    private final int messageId;

    public CallbackQueryEvent(MTProtoTelegramClient client, long queryId, User user,
                              Id peerId, int msgId, long chatInstance,
                              @Nullable ByteBuf data, @Nullable String gameShortName) {
        super(client, queryId, user, chatInstance, data, gameShortName);
        this.messageId = msgId;
        this.peerId = peerId;
    }

    public int getMessageId() {
        return messageId;
    }

    public Id getPeerId() {
        return peerId;
    }

    public Mono<Boolean> answer(AnswerCallbackQuerySpec spec) {
        return Mono.defer(() -> client.getServiceHolder().getMessageService()
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
        return "CallbackQueryEvent{" +
                "peerId=" + peerId +
                ", messageId=" + messageId +
                "} " + super.toString();
    }
}
