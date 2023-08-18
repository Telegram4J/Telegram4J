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
package telegram4j.core.event.domain.message;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.MentionablePeer;
import telegram4j.core.object.Message;
import telegram4j.core.object.chat.AdminRight;
import telegram4j.core.object.chat.Chat;

import java.util.Objects;
import java.util.Optional;

/** Event of edited chat/channel message. */
public final class EditMessageEvent extends MessageEvent {

    private final Message newMessage;
    @Nullable
    private final Message oldMessage;
    @Nullable
    private final Chat chat;
    @Nullable
    private final MentionablePeer author;

    public EditMessageEvent(MTProtoTelegramClient client, Message newMessage,
                            @Nullable Message oldMessage, @Nullable Chat chat,
                            @Nullable MentionablePeer author) {
        super(client);
        this.newMessage = Objects.requireNonNull(newMessage);
        this.oldMessage = oldMessage;
        this.chat = chat;
        this.author = author;
    }

    /**
     * Gets current version of message.
     *
     * @return The current {@link Message} version.
     */
    public Message getCurrentMessage() {
        return newMessage;
    }

    /**
     * Gets previous version of message, if message was previously stored.
     *
     * @return The previous {@link Message} version, if present.
     */
    public Optional<Message> getOldMessage() {
        return Optional.ofNullable(oldMessage);
    }

    /**
     * Gets chat, where message was edited, if present.
     *
     * @return The chat, where message was edited, if present
     */
    public Optional<Chat> getChat() {
        return Optional.ofNullable(chat);
    }

    /**
     * Gets author of message, if present.
     *
     * <p> This peer may have different id rather than {@link Message#getAuthorId()} if
     * real author of message is admin and have {@link AdminRight#ANONYMOUS} permission the special
     * user with id {@link MTProtoTelegramClient#getGroupAnonymousBotId()} may be passed or
     * if message is a channel post then {@link MTProtoTelegramClient#getChannelBotId()}.
     *
     * @return The author entity of message, if present.
     */
    public Optional<MentionablePeer> getAuthor() {
        return Optional.ofNullable(author);
    }

    @Override
    public String toString() {
        return "EditMessageEvent{" +
                "newMessage=" + newMessage +
                ", oldMessage=" + oldMessage +
                ", chat=" + chat +
                ", author=" + author +
                '}';
    }
}
