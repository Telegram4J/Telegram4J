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
package telegram4j.core.object;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.retriever.EntityRetriever;
import telegram4j.core.util.Id;

import java.util.Objects;

/**
 * Representation of certain peer reaction to the message.
 *
 * @see <a href="https://core.telegram.org/api/reactions">Message Reactions</a>
 */
public class MessagePeerReaction implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.MessagePeerReaction data;

    public MessagePeerReaction(MTProtoTelegramClient client, telegram4j.tl.MessagePeerReaction data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets whether the reaction should elicit a bigger and longer animation.
     *
     * @return {@code true} if the reaction should elicit a bigger and longer animation.
     */
    public boolean isBig() {
        return data.big();
    }

    /**
     * Gets whether the reaction wasn't yet marked as read by the <i>current</i> user.
     *
     * @return {@code true} if the reaction wasn't yet marked as read by the <i>current</i> user.
     */
    public boolean isUnread() {
        return data.unread();
    }

    /**
     * Gets id of user or channel that reacted to the message.
     *
     * @return The id of user or channel that reacted to the message.
     */
    public Id getPeerId() {
        return Id.of(data.peerId());
    }

    /**
     * Requests to retrieve peer that reacted to the message.
     *
     * @return A {@link Mono} which emits on successful completion {@link User} or {@link Channel}.
     */
    public Mono<MentionablePeer> getPeer() {
        return getPeer(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve peer that reacted to the message using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return A {@link Mono} which emits on successful completion {@link User} or {@link Channel}.
     */
    public Mono<MentionablePeer> getPeer(EntityRetrievalStrategy strategy) {
        return Mono.defer(() -> {
            Id peerId = getPeerId();
            EntityRetriever retriever = client.withRetrievalStrategy(strategy);

            return switch (peerId.getType()) {
                case USER -> retriever.getUserById(peerId);
                case CHANNEL -> retriever.getChatById(peerId);
                default -> throw new IllegalStateException("Unexpected type of peer id");
            };
        })
        .cast(MentionablePeer.class);
    }

    /**
     * Gets custom or unicode emoji reaction.
     *
     * @return The custom or unicode emoji reaction.
     */
    public Reaction getReaction() {
        return EntityFactory.createReaction(data.reaction());
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public String toString() {
        return "MessagePeerReaction{" +
                "data=" + data +
                '}';
    }
}
