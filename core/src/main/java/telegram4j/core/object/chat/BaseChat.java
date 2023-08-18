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
package telegram4j.core.object.chat;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;
import telegram4j.core.spec.ForwardMessagesSpec;
import telegram4j.core.spec.SendMessageSpec;
import telegram4j.core.util.Id;
import telegram4j.core.util.PeerId;
import telegram4j.tl.messages.AffectedHistory;

import java.util.Objects;
import java.util.Optional;

/**
 * This class provides default implementation of {@link Chat} methods.
 */
sealed abstract class BaseChat implements Chat
        permits BaseChannel, BaseUnavailableChat, GroupChat, PrivateChat {

    protected final MTProtoTelegramClient client;

    protected BaseChat(MTProtoTelegramClient client) {
        this.client = Objects.requireNonNull(client);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    @Override
    public abstract Id getId();

    @Override
    public abstract Type getType();

    @Override
    public abstract Optional<String> getAbout();

    // Interaction methods implementation

    @Override
    public Mono<Message> sendMessage(SendMessageSpec spec) {
        return client.getPeerHandle().sendMessage(getId(), spec);
    }

    @Override
    public Flux<Message> forwardMessages(ForwardMessagesSpec spec, PeerId toPeer) {
        return client.getPeerHandle().forwardMessages(getId(), toPeer, spec);
    }

    @Override
    public Mono<AffectedHistory> unpinAllMessages() {
        return unpinAllMessages0(null);
    }

    protected Mono<AffectedHistory> unpinAllMessages0(@Nullable Integer topMessageId) {
        return client.getPeerHandle().unpinAllMessages(getId(), topMessageId);
    }

    @Override
    public final boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseChat that)) return false;
        return getId().equals(that.getId());
    }

    @Override
    public final int hashCode() {
        return getId().hashCode();
    }
}
