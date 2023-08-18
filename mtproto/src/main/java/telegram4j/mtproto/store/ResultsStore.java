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
package telegram4j.mtproto.store;

import reactor.core.publisher.Mono;
import telegram4j.tl.Chat;
import telegram4j.tl.Config;
import telegram4j.tl.User;
import telegram4j.tl.auth.BaseAuthorization;
import telegram4j.tl.channels.BaseChannelParticipants;
import telegram4j.tl.channels.ChannelParticipant;
import telegram4j.tl.messages.Messages;

public interface ResultsStore {

    /**
     * Applies given peer entities to local store.
     *
     * @param chats An iterable with chats.
     * @param users An iterable with users.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> onContacts(Iterable<? extends Chat> chats, Iterable<? extends User> users);

    /**
     * Applies given full user to local store.
     *
     * @param payload The user full payload.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> onUserUpdate(telegram4j.tl.users.UserFull payload);

    /**
     * Applies given full chat to local store.
     *
     * @param payload The user chat payload.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> onChatUpdate(telegram4j.tl.messages.ChatFull payload);

    /**
     * Applies given channel participants list to local store.
     *
     * @param channelId The id of channel.
     * @param payload The channel participants list.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> onChannelParticipants(long channelId, BaseChannelParticipants payload);

    /**
     * Applies given channel participant to local store.
     *
     * @param channelId The id of channel.
     * @param payload The channel participant.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> onChannelParticipant(long channelId, ChannelParticipant payload);

    /**
     * Applies given messages list to local store.
     *
     * @param payload The messages list.
     * @return A {@link Mono} completing the operation is done.
     */
    Mono<Void> onMessages(Messages payload);

    Mono<Void> onAuthorization(BaseAuthorization auth);

    Mono<Void> onUpdateConfig(Config config);
}
