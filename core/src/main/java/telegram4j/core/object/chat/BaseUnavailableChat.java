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

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.object.PeerNotifySettings;
import telegram4j.core.object.Photo;
import telegram4j.core.object.ProfilePhoto;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.tl.InputMessagePinned;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

sealed abstract class BaseUnavailableChat extends BaseChat
        implements UnavailableChat
        permits UnavailableChannel, UnavailableGroupChat {

    protected BaseUnavailableChat(MTProtoTelegramClient client) {
        super(client);
    }

    @Override
    public abstract Id getId();

    @Override
    public abstract Type getType();

    @Override
    public abstract String getName();

    @Override
    public Optional<ProfilePhoto> getMinPhoto() {
        return Optional.empty();
    }

    @Override
    public Optional<Photo> getPhoto() {
        return Optional.empty();
    }

    @Override
    public Optional<Duration> getMessageAutoDeleteDuration() {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getPinnedMessageId() {
        return Optional.empty();
    }

    @Override
    public Mono<AuxiliaryMessages> getPinnedMessage(EntityRetrievalStrategy strategy) {
        return client.withRetrievalStrategy(strategy)
                .getMessages(getId(), List.of(InputMessagePinned.instance()));
    }

    @Override
    public Optional<PeerNotifySettings> getNotifySettings() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getAbout() {
        return Optional.empty();
    }

    @Override
    public Optional<Integer> getFolderId() {
        return Optional.empty();
    }

    @Override
    public Optional<String> getThemeEmoticon() {
        return Optional.empty();
    }
}
