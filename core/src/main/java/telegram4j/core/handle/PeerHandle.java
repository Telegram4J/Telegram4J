/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package telegram4j.core.handle;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.EntityFactory;
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
import java.util.stream.Collectors;

import static reactor.function.TupleUtils.function;
import static telegram4j.mtproto.util.TlEntityUtil.unmapEmpty;

public class PeerHandle extends EntityHandle {

    protected final MTProtoPeerHandle mtProtoPeerHandle;

    public PeerHandle(MTProtoTelegramClient client, MTProtoPeerHandle mtprotoHandle) {
        super(client);
        this.mtProtoPeerHandle = Objects.requireNonNull(mtprotoHandle);
    }

    public final MTProtoPeerHandle getMTProtoHandle() {
        return mtProtoPeerHandle;
    }

    public Mono<Message> sendMessage(Id peerId, SendMessageSpec spec) {
        return client.asInputPeerExact(peerId).flatMap(peer -> {
            String trimmedMessage = spec.message().trim();
            var parser = spec.parser()
                    .or(() -> client.getMtProtoResources().getDefaultEntityParser())
                    .map(m -> EntityParserSupport.parse(client, m.apply(trimmedMessage)))
                    .orElseGet(() -> Mono.just(Tuples.of(trimmedMessage, List.of())));

            var replyTo = Mono.justOrEmpty(spec.replyTo())
                    .flatMap(replyToSpec -> replyToSpec.resolve(client));

            var replyMarkup = Mono.justOrEmpty(spec.replyMarkup())
                    .flatMap(r -> r.asData(client));

            var sendAs = Mono.justOrEmpty(spec.sendAs())
                    .flatMap(client::resolvePeer)
                    .flatMap(p -> client.asInputPeerExact(p.getId()));

            Integer scheduleDate = spec.scheduleTimestamp()
                    .map(Instant::getEpochSecond)
                    .map(Math::toIntExact)
                    .orElse(null);

            var media = spec.media().orElse(null);
            if (media != null) {
                return Mono.fromSupplier(() -> SendMedia.builder()
                                .randomId(CryptoUtil.random.nextLong())
                                .peer(peer)
                                .flags(spec.flags().getValue())
                                .scheduleDate(scheduleDate))
                        .flatMap(builder -> media.resolve(client)
                                .map(builder::media))
                        .flatMap(builder -> parser
                                .map(function((text, entities) -> builder.message(text)
                                        .entities(entities.isEmpty() ? null : entities))))
                        .flatMap(builder -> replyTo
                                .map(builder::replyTo)
                                .defaultIfEmpty(builder))
                        .flatMap(builder -> replyMarkup
                                .map(builder::replyMarkup)
                                .defaultIfEmpty(builder))
                        .flatMap(builder -> sendAs
                                .map(builder::sendAs)
                                .defaultIfEmpty(builder))
                        .flatMap(builder -> mtProtoPeerHandle.sendMedia(builder.build()))
                        .map(e -> EntityFactory.createMessage(client, e, Id.of(peer, client.getSelfId())));
            }

            return Mono.fromSupplier(() -> SendMessage.builder()
                            .randomId(CryptoUtil.random.nextLong())
                            .peer(peer)
                            .flags(spec.flags().getValue())
                            .scheduleDate(scheduleDate))
                    .flatMap(builder -> parser
                            .map(function((text, entities) -> builder.message(text)
                                    .entities(entities.isEmpty() ? null : entities))))
                    .flatMap(builder -> replyTo
                            .map(builder::replyTo)
                            .defaultIfEmpty(builder))
                    .flatMap(builder -> replyMarkup
                            .map(builder::replyMarkup)
                            .defaultIfEmpty(builder))
                    .flatMap(builder -> sendAs
                            .map(builder::sendAs)
                            .defaultIfEmpty(builder))
                    .flatMap(builder -> mtProtoPeerHandle.sendMessage(builder.build()))
                    .map(e -> EntityFactory.createMessage(client, e, Id.of(peer, client.getSelfId())));
        });
    }

    public Mono<AffectedHistory> unpinAllMessages(Id peerId, @Nullable Integer topMessageId) {
        return client.asInputPeerExact(peerId)
                .flatMap(inputPeer -> mtProtoPeerHandle.unpinAllMessages(inputPeer, topMessageId));
    }

    public Flux<Message> forwardMessages(Id fromPeerId, PeerId toPeerId, ForwardMessagesSpec spec) {
        return Flux.defer(() -> {
            var fromPeerMono = client.asInputPeerExact(fromPeerId);

            var sendAsMono = Mono.justOrEmpty(spec.sendAs())
                    .flatMap(client::resolvePeer)
                    .flatMap(p -> client.asInputPeerExact(p.getId()))
                    .defaultIfEmpty(InputPeerEmpty.instance());

            var toPeerMono = client.resolvePeer(toPeerId)
                    .flatMap(p -> client.asInputPeerExact(p.getId()));

            return Mono.zip(fromPeerMono, toPeerMono, sendAsMono)
                    .flatMapMany(function((fromPeer, toPeerResend, sendAs) -> {
                        Id resolvedChatId = Id.of(toPeerResend, client.getSelfId());
                        return mtProtoPeerHandle.forwardMessages(ForwardMessages.builder()
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
}
