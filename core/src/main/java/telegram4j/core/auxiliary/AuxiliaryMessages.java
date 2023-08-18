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
package telegram4j.core.auxiliary;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.util.Id;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Container with found {@link Message}s with auxiliary {@link Chat} and {@link User} objects. */
public sealed class AuxiliaryMessages
        permits AuxiliaryChannelMessages, AuxiliaryMessagesSlice {

    private final MTProtoTelegramClient client;
    private final List<Message> messages;
    private final Map<Id, Chat> chats;
    private final Map<Id, User> users;

    public AuxiliaryMessages(MTProtoTelegramClient client, List<Message> messages,
                             Map<Id, Chat> chats, Map<Id, User> users) {
        this.client = client;
        this.messages = messages;
        this.chats = chats;
        this.users = users;
    }

    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets immutable list of found {@link Message}s.
     *
     * @return The immutable {@link List} of found {@link Message}s.
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Gets immutable map of {@link Chat}s mentioned in messages.
     * This map doesn't contain {@link PrivateChat} objects.
     *
     * @return The immutable {@link Map} of {@link Chat} mentioned in messages.
     */
    public Map<Id, Chat> getChats() {
        return chats;
    }

    public Optional<Chat> getChat(Id userId) {
        return Optional.ofNullable(chats.get(userId));
    }

    /**
     * Gets immutable map of {@link User}s mentioned in messages.
     *
     * @return The immutable {@link Map} of {@link User} mentioned in messages.
     */
    public Map<Id, User> getUsers() {
        return users;
    }

    public Optional<User> getUser(Id userId) {
        return Optional.ofNullable(users.get(userId));
    }

    @Override
    public String toString() {
        return "AuxiliaryMessages{" +
                "messages=" + messages +
                ", chats=" + chats +
                ", users=" + users +
                '}';
    }
}
