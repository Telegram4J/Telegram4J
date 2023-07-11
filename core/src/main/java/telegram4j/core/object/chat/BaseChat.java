package telegram4j.core.object.chat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.Message;
import telegram4j.core.spec.ForwardMessagesSpec;
import telegram4j.core.spec.SendMessageSpec;
import telegram4j.core.util.Id;
import telegram4j.core.util.PeerId;
import telegram4j.core.util.parser.EntityParserSupport;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.InputPeerEmpty;
import telegram4j.tl.messages.AffectedHistory;
import telegram4j.tl.request.messages.ForwardMessages;
import telegram4j.tl.request.messages.SendMedia;
import telegram4j.tl.request.messages.SendMessage;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static reactor.function.TupleUtils.function;
import static telegram4j.mtproto.util.TlEntityUtil.unmapEmpty;

/** This class provides default implementation of {@link Chat} methods. */
sealed abstract class BaseChat implements Chat
        permits BaseChannel, BaseUnavailableChat, GroupChat, PrivateChat {

    protected final MTProtoTelegramClient client;

    protected BaseChat(MTProtoTelegramClient client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public abstract Id getId();

    @Override
    public abstract Type getType();

    @Override
    public abstract Optional<String> getAbout();

    // Interaction methods implementation

    @Override
    public Mono<Message> sendMessage(SendMessageSpec spec) {
        Id id = getId();
        return client.asInputPeer(id).switchIfEmpty(MappingUtil.unresolvedPeer(id)).flatMap(peer -> {
            var parser = spec.parser()
                    .or(() -> client.getMtProtoResources().getDefaultEntityParser())
                    .map(m -> EntityParserSupport.parse(client, m.apply(spec.message().trim())))
                    .orElseGet(() -> Mono.just(Tuples.of(spec.message(), List.of())));

            var replyMarkup = Mono.justOrEmpty(spec.replyMarkup())
                    .flatMap(r -> r.asData(client));

            var sendAs = Mono.justOrEmpty(spec.sendAs())
                    .flatMap(client::resolvePeer)
                    .flatMap(p -> client.asInputPeer(p.getId())
                            .switchIfEmpty(MappingUtil.unresolvedPeer(p.getId())));

            Integer scheduleDate = spec.scheduleTimestamp()
                    .map(Instant::getEpochSecond)
                    .map(Math::toIntExact)
                    .orElse(null);

            var media = spec.media().orElse(null);
            if (media != null) {
                return Mono.zip(media.asData(client), parser,
                        (resolvedMedia, tuple) -> Tuples.of(resolvedMedia, tuple.getT1(), tuple.getT2()))
                        .map(function((resolvedMedia, txt, ent) -> SendMedia.builder()
                                .media(resolvedMedia)
                                .randomId(CryptoUtil.random.nextLong())
                                .peer(peer)
                                .flags(spec.flags().getValue())
                                .replyToMsgId(spec.replyToMessageId().orElse(null))
                                .message(txt)
                                .entities(ent.isEmpty() ? null : ent)
                                .scheduleDate(scheduleDate)))
                        .<SendMedia>flatMap(builder -> replyMarkup.doOnNext(builder::replyMarkup)
                                .then(sendAs.doOnNext(builder::sendAs))
                                .then(Mono.fromSupplier(builder::build)))
                        .flatMap(client.getServiceHolder().getChatService()::sendMedia)
                        .map(e -> EntityFactory.createMessage(client, e, Id.of(peer, client.getSelfId())));
            }

            return parser.map(function((txt, ent) -> SendMessage.builder()
                            .randomId(CryptoUtil.random.nextLong())
                            .peer(peer)
                            .flags(spec.flags().getValue())
                            .replyToMsgId(spec.replyToMessageId().orElse(null))
                            .message(txt)
                            .entities(ent.isEmpty() ? null : ent)
                            .scheduleDate(scheduleDate)))
                    .<SendMessage>flatMap(builder -> replyMarkup.doOnNext(builder::replyMarkup)
                            .then(sendAs.doOnNext(builder::sendAs))
                            .then(Mono.fromSupplier(builder::build)))
                    .flatMap(client.getServiceHolder().getChatService()::sendMessage)
                    .map(e -> EntityFactory.createMessage(client, e, Id.of(peer, client.getSelfId())));
        });
    }

    @Override
    public Flux<Message> forwardMessages(ForwardMessagesSpec spec, PeerId toPeer) {
        return Flux.defer(() -> {
            Id id = getId();
            var fromPeerMono = client.asInputPeer(id)
                    .switchIfEmpty(MappingUtil.unresolvedPeer(id));

            var sendAsMono = Mono.justOrEmpty(spec.sendAs())
                    .flatMap(client::resolvePeer)
                    .flatMap(p -> client.asInputPeer(p.getId())
                            .switchIfEmpty(MappingUtil.unresolvedPeer(p.getId())))
                    .defaultIfEmpty(InputPeerEmpty.instance());

            var toPeerMono = client.resolvePeer(toPeer)
                    .flatMap(p -> client.asInputPeer(p.getId())
                            .switchIfEmpty(MappingUtil.unresolvedPeer(p.getId())));

            return Mono.zip(fromPeerMono, toPeerMono, sendAsMono)
                    .flatMapMany(function((fromPeer, toPeerResend, sendAs) -> {
                        Id resolvedChatId = Id.of(toPeerResend, client.getSelfId());
                        return client.getServiceHolder().getChatService()
                                .forwardMessages(ForwardMessages.builder()
                                        .id(spec.ids())
                                        .randomId(CryptoUtil.random.longs(spec.ids().size())
                                                .boxed()
                                                .collect(Collectors.toList()))
                                        .flags(spec.flags().getValue())
                                        .fromPeer(fromPeer)
                                        .toPeer(toPeerResend)
                                        .sendAs(unmapEmpty(sendAs))
                                        .scheduleDate(spec.scheduleTimestamp()
                                                .map(Instant::getEpochSecond)
                                                .map(Math::toIntExact)
                                                .orElse(null))
                                        .build())
                                .map(e -> EntityFactory.createMessage(client, e, resolvedChatId));
                    }));
        });
    }

    @Override
    public Mono<AffectedHistory> unpinAllMessages() {
        return unpinAllMessages0(null);
    }

    protected Mono<AffectedHistory> unpinAllMessages0(@Nullable Integer topMessageId) {
        Id id = getId();
        return client.asInputPeer(id)
                .switchIfEmpty(MappingUtil.unresolvedPeer(id))
                .flatMap(peer -> client.getServiceHolder().getChatService().unpinAllMessages(peer, topMessageId));
    }

    @Override
    public final boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseChat that)) return false;
        return getId().equals(that.getId());
    }

    @Override
    public final int hashCode() {
        return getId().hashCode();
    }
}
