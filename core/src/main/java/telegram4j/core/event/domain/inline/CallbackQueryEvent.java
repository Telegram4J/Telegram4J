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
package telegram4j.core.event.domain.inline;

import io.netty.buffer.ByteBuf;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.tl.ImmutableInputMessageID;

import java.util.List;

/** Event of ordinary inline button callback. */
public final class CallbackQueryEvent extends CallbackEvent {
    private final int messageId;
    private final Chat chat;

    public CallbackQueryEvent(MTProtoTelegramClient client, long queryId, User user,
                              Chat chat, int msgId, long chatInstance,
                              @Nullable ByteBuf data, @Nullable String gameShortName) {
        super(client, queryId, user, chatInstance, data, gameShortName);
        this.messageId = msgId;
        this.chat = chat;
    }

    /**
     * Gets id of message where callback was triggered.
     *
     * @return The id of message where callback was triggered.
     */
    public int getMessageId() {
        return messageId;
    }

    /**
     * Requests to retrieve original message.
     *
     * @return An {@link Mono} emitting on successful completion the {@link AuxiliaryMessages message container}.
     */
    public Mono<AuxiliaryMessages> getMessage() {
        return getMessage(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve original message using specified retrieval strategy.
     *
     * @param strategy The strategy to apply.
     * @return An {@link Mono} emitting on successful completion the {@link AuxiliaryMessages message container}.
     */
    public Mono<AuxiliaryMessages> getMessage(EntityRetrievalStrategy strategy) {
        return client.withRetrievalStrategy(strategy)
                .getMessages(chat.getId(), List.of(ImmutableInputMessageID.of(messageId)));
    }

    /**
     * Gets chat where this callback was triggered.
     *
     * @return The {@link Chat} where this callback was triggered.
     */
    public Chat getChat() {
        return chat;
    }

    @Override
    public String toString() {
        return "CallbackQueryEvent{" +
                "messageId=" + messageId +
                ", chat=" + chat +
                "} " + super.toString();
    }
}
