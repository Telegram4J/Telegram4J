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
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.tl.MessageReplyStoryHeader;

import java.util.Objects;

public final class MessageReplyToStoryHeader extends MessageReplyHeader {

    private final MessageReplyStoryHeader data;

    public MessageReplyToStoryHeader(MTProtoTelegramClient client, MessageReplyStoryHeader data) {
        super(client);
        this.data = Objects.requireNonNull(data);
    }

    public Id getUserId() {
        return Id.ofUser(data.userId());
    }

    public Mono<User> getUser() {
        return getUser(MappingUtil.IDENTITY_RETRIEVER);
    }

    public Mono<User> getUser(EntityRetrievalStrategy strategy) {
        return client.withRetrievalStrategy(strategy)
                .getUserById(getUserId());
    }

    public int getStoryId() {
        return data.storyId();
    }

    @Override
    public String toString() {
        return "MessageReplyToStoryHeader{" +
                "data=" + data +
                '}';
    }
}
