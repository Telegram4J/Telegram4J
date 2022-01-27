package telegram4j.core.object.chat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;
import telegram4j.core.object.User;
import telegram4j.core.object.*;
import telegram4j.core.spec.ForwardMessagesSpec;
import telegram4j.core.spec.MessageFields;
import telegram4j.core.spec.SendMediaSpec;
import telegram4j.core.spec.SendMessageSpec;
import telegram4j.core.spec.media.InputMediaPollSpec;
import telegram4j.core.spec.media.InputMediaSpec;
import telegram4j.core.util.EntityFactory;
import telegram4j.core.util.EntityParserSupport;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.*;
import telegram4j.tl.request.messages.ForwardMessages;
import telegram4j.tl.request.messages.SendMedia;
import telegram4j.tl.request.messages.SendMessage;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** This class provides default implementation of {@link Chat} methods. */
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

    protected static InputPeer asInputPeer(PeerEntity entity) {
        if (entity instanceof GroupChat) {
            GroupChat groupChat = (GroupChat) entity;

            return ImmutableInputPeerChat.of(groupChat.getId().asLong());
        } else if (entity instanceof Channel) {
            Channel channel = (Channel) entity;

            Id channelId = channel.getId();
            return ImmutableInputPeerChannel.of(channelId.asLong(), channelId.getAccessHash().orElseThrow());
        }
        // or user

        User user = (User) entity;
        if (user.getFlags().contains(User.Flag.SELF)) {
            return InputPeerSelf.instance();
        }

        Id userId = user.getId();
        return ImmutableInputPeerUser.of(userId.asLong(), userId.getAccessHash().orElseThrow());
    }

    @Override
    public Mono<Message> sendMessage(SendMessageSpec spec) {
        return Mono.defer(() -> {
            var text = spec.parser()
                    .or(() -> client.getMtProtoResources().getDefaultEntityParser())
                    .map(m -> EntityParserSupport.parse(m.apply(spec.message())))
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
                            .message(text.getT1())
                            .entities(text.getT2())
                            .replyMarkup(spec.replyMarkup().map(MessageFields.ReplyMarkupSpec::asData).orElse(null))
                            .scheduleDate(spec.scheduleTimestamp()
                                    .map(Instant::getEpochSecond)
                                    .map(Math::toIntExact)
                                    .orElse(null))
                            .build())
                    .map(e -> EntityFactory.createMessage(client, e, id));
        });
    }

    @Override
    public Flux<Message> forwardMessages(ForwardMessagesSpec spec, PeerId toPeer) {
        return client.resolvePeer(toPeer)
                .zipWith(Mono.justOrEmpty(spec.sendAs())
                        .flatMap(client::resolvePeer)
                        .map(BaseChat::asInputPeer)
                        .defaultIfEmpty(InputPeerEmpty.instance()))
                .flatMapMany(TupleUtils.function((toPeerRe, sendAs) -> client.getServiceHolder()
                        .getMessageService()
                        .forwardMessages(ForwardMessages.builder()
                                .id(spec.ids())
                                .randomId(CryptoUtil.random.longs(spec.ids().size())
                                        .boxed()
                                        .collect(Collectors.toList()))
                                .silent(spec.silent())
                                .background(spec.background())
                                .withMyScore(spec.withMyScore())
                                .dropAuthor(spec.dropAuthor())
                                .dropMediaCaptions(spec.dropMediaCaptions())
                                .noforwards(spec.noForwards())
                                .fromPeer(getIdAsPeer())
                                .silent(spec.silent())
                                .toPeer(asInputPeer(toPeerRe))
                                .sendAs(sendAs.identifier() == InputPeerEmpty.ID ? null : sendAs)
                                .scheduleDate(spec.scheduleTimestamp()
                                        .map(Instant::getEpochSecond)
                                        .map(Math::toIntExact)
                                        .orElse(null))
                                .build())
                        .map(e -> EntityFactory.createMessage(client, e, toPeerRe.getId()))));
    }

    @Override
    public Mono<Message> sendMedia(SendMediaSpec spec) {
        return Mono.justOrEmpty(spec.sendAs())
                .flatMap(client::resolvePeer)
                .map(BaseChat::asInputPeer)
                .defaultIfEmpty(InputPeerEmpty.instance())
                .zipWith(Mono.fromSupplier(() -> awareEntityParser(spec.media())))
                .flatMap(TupleUtils.function((sendAs, media) -> client.getServiceHolder()
                        .getMessageService()
                        .sendMedia(SendMedia.builder()
                                .media(media.asData())
                                .randomId(CryptoUtil.random.nextLong())
                                .silent(spec.silent())
                                .background(spec.background())
                                .clearDraft(spec.clearDraft())
                                .noforwards(spec.noForwards())
                                .peer(getIdAsPeer())
                                .silent(spec.silent())
                                .message(spec.message())
                                .sendAs(sendAs.identifier() == InputPeerEmpty.ID ? null : sendAs)
                                .scheduleDate(spec.scheduleTimestamp()
                                        .map(Instant::getEpochSecond)
                                        .map(Math::toIntExact)
                                        .orElse(null))
                                .build())
                        .map(e -> EntityFactory.createMessage(client, e, id))));
    }

    private InputMediaSpec awareEntityParser(InputMediaSpec spec) {
        if (spec.type() == InputMediaSpec.Type.POLL) {
            InputMediaPollSpec media = (InputMediaPollSpec) spec;

            return media.withParser(media.parser().or(() -> client.getMtProtoResources().getDefaultEntityParser()));
        }
        return spec;
    }

    @Override
    public Mono<MessageMedia> uploadMedia(InputMediaSpec spec) {
        return Mono.fromSupplier(() -> awareEntityParser(spec))
                .flatMap(media -> client.getServiceHolder().getMessageService()
                        .uploadMedia(getIdAsPeer(), media.asData()));
    }
}
