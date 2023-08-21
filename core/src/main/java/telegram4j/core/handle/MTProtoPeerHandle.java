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
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.DcId;
import telegram4j.tl.*;
import telegram4j.tl.messages.AffectedHistory;
import telegram4j.tl.request.messages.ForwardMessages;
import telegram4j.tl.request.messages.SendMedia;
import telegram4j.tl.request.messages.SendMessage;
import telegram4j.tl.request.messages.UnpinAllMessages;

import java.util.Objects;

public class MTProtoPeerHandle extends MTProtoHandle {

    public MTProtoPeerHandle(MTProtoTelegramClient client) {
        super(client);
    }

    public Flux<Message> forwardMessages(ForwardMessages request) {
        return client.getMtProtoClientGroup().send(DcId.main(), request)
                .cast(BaseUpdates.class)
                .flatMapMany(updates -> {
                    client.getMtProtoClientGroup().updates().publish(updates);

                    return Flux.fromIterable(updates.updates())
                            .mapNotNull(u -> {
                                Message m;
                                if (u instanceof UpdateNewMessage n) {
                                    m = n.message();
                                } else if (u instanceof UpdateNewChannelMessage n) {
                                    m = n.message();
                                } else {
                                    throw new IllegalStateException("Unexpected type of update: " + u);
                                }

                                if (m instanceof MessageEmpty) {
                                    throw new IllegalStateException("Received MessageEmpty on ForwardMessages updates");
                                }
                                return m;
                            });
                });
    }

    public Mono<AffectedHistory> unpinAllMessages(InputPeer inputPeer, @Nullable Integer topMessageId) {
        return client.getMtProtoClientGroup().send(DcId.main(), UnpinAllMessages.builder()
                .peer(inputPeer)
                .topMsgId(topMessageId)
                .build());
    }

    public Mono<Message> sendMedia(SendMedia request) {
        return client.getMtProtoClientGroup().send(DcId.main(), request)
                .map(u -> transformMessageUpdate(request, u))
                .flatMap(message -> {
                    if (request.media() instanceof InputMediaPoll m) {
                        var messageCasted = (BaseMessage) message;
                        var messageMediaPoll = (MessageMediaPoll) Objects.requireNonNull(messageCasted.media());
                        return client.getMtProtoResources().getStoreLayout().registerPoll(messageCasted.peerId(), messageCasted.id(),
                                ImmutableInputMediaPoll.copyOf(m)
                                        .withPoll(ImmutablePoll.copyOf(messageMediaPoll.poll())))
                                .thenReturn(messageCasted);
                    }

                    return Mono.just(message);
                });
    }

    public Mono<Message> sendMessage(SendMessage request) {
        return client.getMtProtoClientGroup().send(DcId.main(), request)
                .map(u -> transformMessageUpdate(request, u));
    }

    // Short-send related updates object should be transformed to the updateShort or baseUpdates.
    // https://core.telegram.org/api/updates#updates-sequence

    private Peer authorOf(SendMessage request) {
        var sendAs = request.sendAs();
        return sendAs != null ? toPeer(sendAs) : toPeer(InputPeerSelf.instance());
    }

    private Peer authorOf(SendMedia request) {
        var sendAs = request.sendAs();
        return sendAs != null ? toPeer(sendAs) : toPeer(InputPeerSelf.instance());
    }

    private Message transformMessageUpdate(SendMedia request, Updates updates) {
        Updates updatesMapped;
        Message message;
        switch (updates.identifier()) {
            case UpdateShortSentMessage.ID -> {
                var casted = (UpdateShortSentMessage) updates;
                var replyTo = request.replyTo();

                message = BaseMessage.builder()
                        .flags(request.flags() | casted.flags())
                        .peerId(toPeer(request.peer()))
                        .fromId(authorOf(request))
                        .replyTo(replyTo != null ? toOutput(replyTo) : null)
                        .message(request.message())
                        .id(casted.id())
                        .replyMarkup(request.replyMarkup())
                        .media(casted.media())
                        .entities(casted.entities())
                        .date(casted.date())
                        .ttlPeriod(casted.ttlPeriod())
                        .build();

                updatesMapped = UpdateShort.builder()
                        .date(casted.date())
                        .update(UpdateNewMessage.builder()
                                .message(message)
                                .pts(casted.pts())
                                .ptsCount(casted.ptsCount())
                                .build())
                        .build();
            }
            case UpdateShortMessage.ID -> {
                var casted = (UpdateShortMessage) updates;

                message = BaseMessage.builder()
                        .flags(request.flags() | casted.flags())
                        .peerId(toPeer(request.peer()))
                        .fromId(authorOf(request))
                        .replyTo(casted.replyTo())
                        .message(request.message())
                        .id(casted.id())
                        .replyMarkup(request.replyMarkup())
                        .fwdFrom(casted.fwdFrom())
                        .entities(casted.entities())
                        .date(casted.date())
                        .viaBotId(casted.viaBotId())
                        .ttlPeriod(casted.ttlPeriod())
                        .build();

                updatesMapped = UpdateShort.builder()
                        .date(casted.date())
                        .update(UpdateNewMessage.builder()
                                .message(message)
                                .pts(casted.pts())
                                .ptsCount(casted.ptsCount())
                                .build())
                        .build();
            }
            case UpdateShortChatMessage.ID -> {
                var casted = (UpdateShortChatMessage) updates;

                message = BaseMessage.builder()
                        .flags(request.flags() | casted.flags())
                        .peerId(toPeer(request.peer()))
                        .fromId(authorOf(request))
                        .viaBotId(casted.viaBotId())
                        .replyTo(casted.replyTo())
                        .fwdFrom(casted.fwdFrom())
                        .message(request.message())
                        .id(casted.id())
                        .replyMarkup(request.replyMarkup())
                        .entities(casted.entities())
                        .date(casted.date())
                        .ttlPeriod(casted.ttlPeriod())
                        .build();

                updatesMapped = UpdateShort.builder()
                        .date(casted.date())
                        .update(UpdateNewMessage.builder()
                                .message(message)
                                .pts(casted.pts())
                                .ptsCount(casted.ptsCount())
                                .build())
                        .build();
            }
            case BaseUpdates.ID -> {
                var casted = (BaseUpdates) updates;

                updatesMapped = updates;

                var updateMessageId = casted.updates().stream()
                        .filter(u -> u.identifier() == UpdateMessageID.ID)
                        .findFirst()
                        .map(upd -> (UpdateMessageID) upd)
                        .orElseThrow();

                if (updateMessageId.randomId() != request.randomId()) {
                    throw new IllegalArgumentException("Incorrect random id. Excepted: " + request.randomId()
                            + ", received: " + updateMessageId.randomId());
                }

                message = null;
                for (Update u : casted.updates()) {
                    Message m;
                    if (u instanceof UpdateNewMessage e) {
                        m = e.message();
                    } else if (u instanceof UpdateNewChannelMessage e) {
                        m = e.message();
                    } else if (u instanceof UpdateNewScheduledMessage e) {
                        m = e.message();
                    } else {
                        continue;
                    }

                    if (m.id() == updateMessageId.id()) {
                        if (m instanceof MessageEmpty) {
                            throw new IllegalStateException("Received MessageEmpty on SendMedia updates");
                        }
                        message = m;
                        break;
                    }
                }

                if (message == null) {
                    throw new IllegalStateException("No message in BaseUpdates: " + casted);
                }
            }
            default -> throw new IllegalArgumentException("Unknown Updates type: " + updates);
        }

        client.getMtProtoClientGroup().updates().publish(updatesMapped);
        return message;
    }

    private MessageReplyHeader toOutput(InputReplyTo input) {
        return switch (input.identifier()) {
            case InputReplyToMessage.ID -> {
                var replyTo = (InputReplyToMessage) input;
                yield BaseMessageReplyHeader.builder()
                        .replyToMsgId(replyTo.replyToMsgId())
                        .replyToTopId(replyTo.topMsgId())
                        .build();
            }
            case InputReplyToStory.ID -> {
                var replyTo = (InputReplyToStory) input;

                yield MessageReplyStoryHeader.builder()
                        .userId(rawIdOf(replyTo.userId()))
                        .storyId(replyTo.storyId())
                        .build();
            }
            default -> throw new IllegalArgumentException("Unexpected InputReplyTo type: " + input);
        };
    }

    private Message transformMessageUpdate(SendMessage request, Updates updates) {
        Updates updatesMapped;
        Message message;
        switch (updates.identifier()) {
            case UpdateShortSentMessage.ID -> {
                var casted = (UpdateShortSentMessage) updates;
                var replyTo = request.replyTo();
                message = BaseMessage.builder()
                        .flags(request.flags() | casted.flags())
                        .peerId(toPeer(request.peer()))
                        .fromId(authorOf(request))
                        .replyTo(replyTo != null ? toOutput(replyTo) : null)
                        .message(request.message())
                        .id(casted.id())
                        .replyMarkup(request.replyMarkup())
                        .media(casted.media())
                        .entities(casted.entities())
                        .date(casted.date())
                        .ttlPeriod(casted.ttlPeriod())
                        .build();

                updatesMapped = UpdateShort.builder()
                        .date(casted.date())
                        .update(UpdateNewMessage.builder()
                                .message(message)
                                .pts(casted.pts())
                                .ptsCount(casted.ptsCount())
                                .build())
                        .build();
            }
            case UpdateShortMessage.ID -> {
                var casted = (UpdateShortMessage) updates;

                message = BaseMessage.builder()
                        .flags(request.flags() | casted.flags())
                        .peerId(toPeer(request.peer()))
                        .fromId(authorOf(request))
                        .replyTo(casted.replyTo())
                        .message(request.message())
                        .id(casted.id())
                        .replyMarkup(request.replyMarkup())
                        .fwdFrom(casted.fwdFrom())
                        .entities(casted.entities())
                        .date(casted.date())
                        .viaBotId(casted.viaBotId())
                        .ttlPeriod(casted.ttlPeriod())
                        .build();

                updatesMapped = UpdateShort.builder()
                        .date(casted.date())
                        .update(UpdateNewMessage.builder()
                                .message(message)
                                .pts(casted.pts())
                                .ptsCount(casted.ptsCount())
                                .build())
                        .build();
            }
            case UpdateShortChatMessage.ID -> {
                var casted = (UpdateShortChatMessage) updates;

                message = BaseMessage.builder()
                        .flags(request.flags() | casted.flags())
                        .peerId(toPeer(request.peer()))
                        .fromId(authorOf(request))
                        .viaBotId(casted.viaBotId())
                        .replyTo(casted.replyTo())
                        .fwdFrom(casted.fwdFrom())
                        .message(request.message())
                        .id(casted.id())
                        .replyMarkup(request.replyMarkup())
                        .entities(casted.entities())
                        .date(casted.date())
                        .ttlPeriod(casted.ttlPeriod())
                        .build();

                updatesMapped = UpdateShort.builder()
                        .date(casted.date())
                        .update(UpdateNewMessage.builder()
                                .message(message)
                                .pts(casted.pts())
                                .ptsCount(casted.ptsCount())
                                .build())
                        .build();
            }
            case BaseUpdates.ID -> {
                var casted = (BaseUpdates) updates;

                updatesMapped = updates;

                var updateMessageId = casted.updates().stream()
                        .filter(u -> u.identifier() == UpdateMessageID.ID)
                        .findFirst()
                        .map(upd -> (UpdateMessageID) upd)
                        .orElseThrow();

                if (updateMessageId.randomId() != request.randomId()) {
                    throw new IllegalArgumentException("Incorrect random id. Excepted: " + request.randomId()
                            + ", received: " + updateMessageId.randomId());
                }

                message = null;
                for (Update u : casted.updates()) {
                    Message m;
                    if (u instanceof UpdateNewMessage e) {
                        m = e.message();
                    } else if (u instanceof UpdateNewChannelMessage e) {
                        m = e.message();
                    } else if (u instanceof UpdateNewScheduledMessage e) {
                        m = e.message();
                    } else {
                        continue;
                    }

                    if (m.id() == updateMessageId.id()) {
                        if (m instanceof MessageEmpty) {
                            throw new IllegalStateException("Received MessageEmpty on SendMedia updates");
                        }
                        message = m;
                        break;
                    }
                }

                if (message == null) {
                    throw new IllegalStateException("No message in BaseUpdates: " + casted);
                }
            }
            default -> throw new IllegalArgumentException("Unknown Updates type: " + updates);
        }

        client.getMtProtoClientGroup().updates().publish(updatesMapped);
        return message;
    }
}
