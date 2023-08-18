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
package telegram4j.core.event.dispatcher;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.util.Id;
import telegram4j.tl.*;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class UpdateContext<U extends Update> {
    private final MTProtoTelegramClient client;
    private final Map<Id, Chat> chats;
    private final Map<Id, User> users;
    private final U update;

    protected UpdateContext(MTProtoTelegramClient client, Map<Id, Chat> chats, Map<Id, User> users, U update) {
        this.client = Objects.requireNonNull(client);
        this.chats = Objects.requireNonNull(chats);
        this.users = Objects.requireNonNull(users);
        this.update = Objects.requireNonNull(update);
    }

    public static <U extends Update> UpdateContext<U> create(MTProtoTelegramClient client, U update) {
        return new UpdateContext<>(client, Map.of(), Map.of(), update);
    }

    public static <U extends Update> UpdateContext<U> create(MTProtoTelegramClient client,
                                                             Map<Id, Chat> chatsMap,
                                                             Map<Id, User> usersMap, U update) {
        return new UpdateContext<>(client, chatsMap, usersMap, update);
    }

    public MTProtoTelegramClient getClient() {
        return client;
    }

    public Map<Id, Chat> getChats() {
        return chats;
    }

    public Map<Id, User> getUsers() {
        return users;
    }

    public Optional<Chat> getChatEntity(Id peer) {
        return switch (peer.getType()) {
            case CHAT, CHANNEL -> Optional.ofNullable(chats.get(peer));
            case USER -> Optional.ofNullable(users.get(peer))
                    .map(u -> new PrivateChat(client, u, users.get(client.getSelfId())));
        };
    }

    public Optional<PeerEntity> getPeer(Id peer) {
        return switch (peer.getType()) {
            case CHAT, CHANNEL -> Optional.ofNullable(chats.get(peer));
            case USER -> Optional.ofNullable(users.get(peer));
        };
    }

    public U getUpdate() {
        return update;
    }
}
