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
package telegram4j.core.retriever;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.util.Id;
import telegram4j.core.util.PeerId;
import telegram4j.tl.InputMessage;

import java.util.Objects;

class FallbackEntityRetriever implements EntityRetriever {
    private final EntityRetriever first;
    private final EntityRetriever second;

    FallbackEntityRetriever(EntityRetriever first, EntityRetriever second) {
        this.first = Objects.requireNonNull(first);
        this.second = Objects.requireNonNull(second);
    }

    @Override
    public Mono<PeerEntity> resolvePeer(PeerId peerId) {
        return first.resolvePeer(peerId)
                .switchIfEmpty(second.resolvePeer(peerId));
    }

    @Override
    public Mono<User> getUserById(Id userId) {
        return first.getUserById(userId)
                .switchIfEmpty(second.getUserById(userId));
    }

    @Override
    public Mono<User> getUserMinById(Id userId) {
        return first.getUserMinById(userId)
                .switchIfEmpty(second.getUserMinById(userId));
    }

    @Override
    public Mono<User> getUserFullById(Id userId) {
        return first.getUserFullById(userId)
                .switchIfEmpty(second.getUserFullById(userId));
    }

    @Override
    public Mono<Chat> getChatById(Id chatId) {
        return first.getChatById(chatId)
                .switchIfEmpty(second.getChatById(chatId));
    }

    @Override
    public Mono<Chat> getChatMinById(Id chatId) {
        return first.getChatMinById(chatId)
                .switchIfEmpty(second.getChatMinById(chatId));
    }

    @Override
    public Mono<Chat> getChatFullById(Id chatId) {
        return first.getChatFullById(chatId)
                .switchIfEmpty(second.getChatFullById(chatId));
    }

    @Override
    public Mono<ChatParticipant> getParticipantById(Id chatId, Id peerId) {
        return first.getParticipantById(chatId, peerId)
                .switchIfEmpty(second.getParticipantById(chatId, peerId));
    }

    @Override
    public Flux<ChatParticipant> getParticipants(Id chatId) {
        return first.getParticipants(chatId)
                .switchIfEmpty(second.getParticipants(chatId));
    }

    @Override
    public Mono<AuxiliaryMessages> getMessages(@Nullable Id chatId, Iterable<? extends InputMessage> messageIds) {
        return first.getMessages(chatId, messageIds)
                .switchIfEmpty(second.getMessages(chatId, messageIds));
    }
}
