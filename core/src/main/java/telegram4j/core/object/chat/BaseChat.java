package telegram4j.core.object.chat;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Id;
import telegram4j.core.object.Message;
import telegram4j.core.object.markup.ReplyMarkup;
import telegram4j.core.spec.SendMessageSpec;
import telegram4j.core.util.EntityFactory;
import telegram4j.core.util.EntityParser;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.ImmutableInputPeerChannel;
import telegram4j.tl.ImmutableInputPeerChat;
import telegram4j.tl.ImmutableInputPeerUser;
import telegram4j.tl.InputPeer;
import telegram4j.tl.request.messages.SendMessage;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

abstract class BaseChat implements Chat {

    protected final MTProtoTelegramClient client;
    protected final Id id;
    protected final Type type;

    protected BaseChat(MTProtoTelegramClient client, Id id, Type type) {
        this.client = Objects.requireNonNull(client, "client");
        this.id = Objects.requireNonNull(id, "id");
        this.type = Objects.requireNonNull(type, "type");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public Id getId() {
        return id;
    }

    @Override
    public Type getType() {
        return type;
    }

    // Interaction methods implementation

    protected InputPeer getIdAsPeer() {
        switch (type) {
            case PRIVATE: return ImmutableInputPeerUser.of(id.asLong(), id.getAccessHash().orElseThrow());
            case SUPERGROUP:
            case CHANNEL: return ImmutableInputPeerChannel.of(id.asLong(), id.getAccessHash().orElseThrow());
            case GROUP: return ImmutableInputPeerChat.of(id.asLong());
            default: throw new IllegalStateException();
        }
    }

    @Override
    public Mono<Message> sendMessage(SendMessageSpec spec) {
        return Mono.defer(() -> {
            var tuple = spec.parseMode()
                    .map(m -> EntityParser.parse(spec.message(), m))
                    .orElseGet(() -> Tuples.of(spec.message(), List.of()));

            return client.getServiceHolder().getMessageService()
                    .sendMessage(SendMessage.builder()
                            .randomId(CryptoUtil.random.nextLong())
                            .peer(getIdAsPeer())
                            .silent(spec.silent())
                            .noWebpage(spec.noWebpage())
                            .background(spec.background())
                            .clearDraft(spec.clearDraft())
                            .replyToMsgId(spec.replyToMessageId().orElse(null))
                            .message(tuple.getT1())
                            .entities(tuple.getT2())
                            .replyMarkup(spec.replyMarkup().map(ReplyMarkup::getData).orElse(null))
                            .scheduleDate(spec.scheduleTimestamp()
                                    .map(Instant::getEpochSecond)
                                    .map(Math::toIntExact)
                                    .orElse(null))
                            .build())
                    .map(e -> EntityFactory.createMessage(client, e, id));
        });
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseChat baseChat = (BaseChat) o;
        return id.equals(baseChat.id) && type == baseChat.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type);
    }

    @Override
    public String toString() {
        return "BaseChat{" +
                "id=" + id +
                ", type=" + type +
                '}';
    }
}
