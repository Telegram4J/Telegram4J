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

import telegram4j.core.MTProtoTelegramClient;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/** Message reactions information. */
public class MessageReactions implements TelegramObject {
    private final MTProtoTelegramClient client;
    private final telegram4j.tl.MessageReactions data;

    public MessageReactions(MTProtoTelegramClient client, telegram4j.tl.MessageReactions data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
    }

    /**
     * Gets whether this is minimal information about reactions.
     *
     * @return {@code true} if it is minimal information about reactions.
     */
    public boolean isMin() {
        return data.min();
    }

    /**
     * Gets whether <i>current</i> user can see detailed list of peers which reacted to the message.
     *
     * @return {@code true} if <i>current</i> user can see detailed list of peers which reacted to the message.
     */
    public boolean isCanSeeList() {
        return data.canSeeList();
    }

    /**
     * Gets list of count of reactions.
     *
     * @return The mutable {@link List} of count of reactions.
     */
    public List<ReactionCount> getResults() {
        return data.results().stream()
                .map(ReactionCount::new)
                .collect(Collectors.toList());
    }

    /**
     * Gets list of recent peers and their reactions.
     *
     * @return The mutable {@link List} of recent peers and their reactions.
     */
    public Optional<List<MessagePeerReaction>> getRecentReactions() {
        return Optional.ofNullable(data.recentReactions())
                .map(list -> list.stream()
                        .map(d -> new MessagePeerReaction(client, d))
                        .collect(Collectors.toList()));
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public String toString() {
        return "MessageReactions{" +
                "data=" + data +
                '}';
    }
}
