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

/**
 * Additional wrapping for {@code EntityRetriever} which have settings to control
 * behavior of {@link #getUserById(Id)} and {@link #getChatById(Id)} methods.
 */
public class PreferredEntityRetriever implements EntityRetriever {
    private final EntityRetriever delegate;
    private final Setting chatPreference;
    private final Setting userPreference;

    PreferredEntityRetriever(EntityRetriever delegate, Setting chatPreference, Setting userPreference) {
        this.delegate = Objects.requireNonNull(delegate);
        this.chatPreference = chatPreference;
        this.userPreference = userPreference;
    }

    @Override
    public Mono<PeerEntity> resolvePeer(PeerId peerId) {
        return delegate.resolvePeer(peerId);
    }

    @Override
    public Mono<User> getUserById(Id userId) {
        return switch (userPreference) {
            case MIN -> delegate.getUserMinById(userId);
            case FULL -> delegate.getUserFullById(userId);
            case NONE -> delegate.getUserById(userId);
        };
    }

    @Override
    public Mono<User> getUserMinById(Id userId) {
        return delegate.getUserMinById(userId);
    }

    @Override
    public Mono<User> getUserFullById(Id userId) {
        return delegate.getUserFullById(userId);
    }

    @Override
    public Mono<Chat> getChatById(Id chatId) {
        return switch (chatPreference) {
            case MIN -> delegate.getChatMinById(chatId);
            case FULL -> delegate.getChatFullById(chatId);
            case NONE -> delegate.getChatById(chatId);
        };
    }

    @Override
    public Mono<Chat> getChatMinById(Id chatId) {
        return delegate.getChatMinById(chatId);
    }

    @Override
    public Mono<Chat> getChatFullById(Id chatId) {
        return delegate.getChatFullById(chatId);
    }

    @Override
    public Mono<ChatParticipant> getParticipantById(Id chatId, Id peerId) {
        return delegate.getParticipantById(chatId, peerId);
    }

    @Override
    public Flux<ChatParticipant> getParticipants(Id chatId) {
        return delegate.getParticipants(chatId);
    }

    @Override
    public Mono<AuxiliaryMessages> getMessages(@Nullable Id chatId, Iterable<? extends InputMessage> messageIds) {
        return delegate.getMessages(chatId, messageIds);
    }

    /** Types of volumes of returned information. */
    public enum Setting {

        /** Option to use delegate's behavior. */
        NONE,

        /** Option for returning min objects. */
        MIN,

        /** Option for returning full objects. */
        FULL
    }
}
