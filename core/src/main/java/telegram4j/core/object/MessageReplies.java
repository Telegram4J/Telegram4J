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

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.chat.SupergroupChat;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Information about message replies.
 *
 * @see <a href="https://core.telegram.org/api/threads">Threads</a>
 */
public class MessageReplies implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.MessageReplies data;

    public MessageReplies(MTProtoTelegramClient client, telegram4j.tl.MessageReplies data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets whether this information related to post comments.
     *
     * @return {@code true} if this information related to post comments.
     */
    public boolean isComments() {
        return data.comments();
    }

    /**
     * Gets number of replies in this thread.
     *
     * @return The number of replies in this thread
     */
    public int getReplies() {
        return data.replies();
    }

    /**
     * Gets channel pts of the message that started this thread.
     *
     * @return The channel pts of the message that started this thread.
     */
    public int getRepliesPts() {
        return data.repliesPts();
    }

    /**
     * Gets list of the last few comment posters ids, if present.
     *
     * @return The mutable {@link Set} of the last few comment posters ids, if present.
     */
    public Set<Id> getRecentRepliersIds() {
        return Optional.ofNullable(data.recentRepliers())
                .map(list -> list.stream()
                        .map(Id::of)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    /**
     * Requests to retrieve the recent comment's posters.
     *
     * @return A {@link Flux} which emits the {@link MentionablePeer peer} entities.
     */
    public Flux<MentionablePeer> getRecentRepliers() {
        return getRecentRepliers(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve the recent comment's posters using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return A {@link Flux} which emits the {@link MentionablePeer peer} entities.
     */
    public Flux<MentionablePeer> getRecentRepliers(EntityRetrievalStrategy strategy) {
        var recentRepliers = data.recentRepliers();
        if (recentRepliers == null) {
            return Flux.empty();
        }
        var retriever = client.withRetrievalStrategy(strategy);
        return Flux.fromIterable(recentRepliers)
                .map(Id::of)
                .flatMap(id -> switch (id.getType()) {
                    case USER -> retriever.getUserById(id);
                    case CHANNEL -> retriever.getChatById(id);
                    default -> Flux.error(new IllegalStateException());
                })
                .cast(MentionablePeer.class);
    }

    /**
     * Gets id of discussion supergroup, if present.
     * Always present when {@link #isComments()} flag is set.
     *
     * @see <a href="https://core.telegram.org/api/discussion">Discussion Supergroups</a>
     * @return The id of discussion supergroup, if present.
     */
    public Optional<Id> getDiscussionChannelId() {
        return Optional.ofNullable(data.channelId()).map(Id::ofChannel);
    }

    /**
     * Requests to retrieve discussion channel.
     *
     * @return An {@link Mono} emitting on successful completion the {@link SupergroupChat discussion channel}.
     */
    public Mono<SupergroupChat> getDiscussionChannel() {
        return getDiscussionChannel(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve discussion channel using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link SupergroupChat discussion channel}.
     */
    public Mono<SupergroupChat> getDiscussionChannel(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getDiscussionChannelId())
                .flatMap(id -> client.withRetrievalStrategy(strategy).getChatById(id))
                .cast(SupergroupChat.class);
    }

    /**
     * Gets id of the latest message in this thread, if present.
     *
     * @return The id of the latest message in this thread, if present.
     */
    public Optional<Integer> getMaxMessageId() {
        return Optional.ofNullable(data.maxId());
    }

    /**
     * Gets id of the latest read message in this thread, if present.
     *
     * @return The id of the latest read message in this thread, if present.
     */
    public Optional<Integer> getReadMaxMessageId() {
        return Optional.ofNullable(data.readMaxId());
    }

    @Override
    public String toString() {
        return "MessageReplies{" +
                "data=" + data +
                '}';
    }
}
