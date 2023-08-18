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
package telegram4j.core.internal;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.MentionablePeer;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.object.chat.UnresolvedPeerException;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.BitFlag;
import telegram4j.core.util.Id;
import telegram4j.core.util.ImmutableEnumSet;
import telegram4j.core.util.Variant2;
import telegram4j.tl.BaseMessage;
import telegram4j.tl.MessageService;
import telegram4j.tl.Peer;

import java.time.Duration;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class MappingUtil {
    private MappingUtil() {}

    public static final EntityRetrievalStrategy IDENTITY_RETRIEVER = client -> client;

    public static <E extends Enum<E> & BitFlag> int getMaskValue(Iterable<E> values) {
        if (values instanceof ImmutableEnumSet<E> e)
            return e.getValue();
        int mask = 0;
        for (E value : values) {
            mask |= value.mask();
        }
        return mask;
    }

    public static Duration durationFromSeconds(double seconds) {
        long secs = (long) seconds;
        long millis = (long) ((seconds - secs) * 1000);
        return Duration.ofSeconds(secs, TimeUnit.MILLISECONDS.toNanos(millis));
    }

    public static <E extends Enum<E>> EnumSet<E> copyAsEnumSet(Class<E> type, Iterable<E> iterable) {
        var set = EnumSet.noneOf(type);
        for (E e : iterable) {
            set.add(e);
        }
        return set;
    }

    public static <T> Mono<T> unresolvedPeer(Id peerId) {
        return Mono.error(() -> new UnresolvedPeerException(peerId));
    }

    public static Optional<MentionablePeer> getAuthor(Variant2<BaseMessage, MessageService> data, @Nullable Chat chat,
                                                      MTProtoTelegramClient client,
                                                      Map<Id, Chat> chatsMap, Map<Id, User> usersMap) {
        return getAuthor0(data.map(BaseMessage::fromId, MessageService::fromId),
                data.map(BaseMessage::out, MessageService::out), chat, client, chatsMap, usersMap);
    }

    public static Optional<MentionablePeer> getAuthor(MessageService message, @Nullable Chat chat,
                                                      MTProtoTelegramClient client,
                                                      Map<Id, Chat> chatsMap, Map<Id, User> usersMap) {
        return getAuthor0(message.fromId(), message.out(), chat, client, chatsMap, usersMap);
    }

    public static Optional<MentionablePeer> getAuthor(BaseMessage message, @Nullable Chat chat,
                                                      MTProtoTelegramClient client,
                                                      Map<Id, Chat> chatsMap, Map<Id, User> usersMap) {
        return getAuthor0(message.fromId(), message.out(), chat, client, chatsMap, usersMap);
    }

    private static Optional<MentionablePeer> getAuthor0(@Nullable Peer fromId, boolean out, @Nullable Chat chat,
                                                        MTProtoTelegramClient client,
                                                        Map<Id, Chat> chatsMap, Map<Id, User> usersMap) {
        return Optional.ofNullable(fromId)
                .map(p -> {
                    Id id = Id.of(p);
                    return switch (id.getType()) {
                        case CHANNEL -> (MentionablePeer) chatsMap.get(id);
                        case USER -> usersMap.get(id);
                        case CHAT -> throw new IllegalStateException();
                    };
                })
                // fromId is often not set if the message was sent to the DM, so we will have to process it for convenience
                .or(() -> Optional.ofNullable(chat)
                        .map(c -> switch (c.getType()) {
                            case PRIVATE -> {
                                var pc = (PrivateChat) c;
                                yield out ? pc.getSelfUser().orElse(null) : pc.getUser();
                            }
                            case CHANNEL -> usersMap.get(client.getChannelBotId());
                            case SUPERGROUP -> usersMap.get(client.getGroupAnonymousBotId());
                            case GROUP -> null;
                        }));
    }
}
