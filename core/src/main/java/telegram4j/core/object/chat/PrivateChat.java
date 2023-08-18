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
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.object.PeerNotifySettings;
import telegram4j.core.object.Photo;
import telegram4j.core.object.ProfilePhoto;
import telegram4j.core.object.User;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/** Represents a direct message chat. */
public final class PrivateChat extends BaseChat implements Chat {

    private final User user;
    private final User selfUser;

    public PrivateChat(MTProtoTelegramClient client, User user, @Nullable User selfUser) {
        super(client);
        this.user = Objects.requireNonNull(user);
        this.selfUser = selfUser;
    }

    /**
     * Gets the interlocutor user.
     *
     * @return The {@link User} interlocutor.
     */
    public User getUser() {
        return user;
    }

    /**
     * Gets the self user, if present.
     *
     * @return The self {@link User} of DM, if present.
     */
    public Optional<User> getSelfUser() {
        return Optional.ofNullable(selfUser);
    }

    @Override
    public Id getId() {
        return user.getId();
    }

    @Override
    public Type getType() {
        return Type.PRIVATE;
    }

    @Override
    public String getName() {
        return user.getFullName();
    }

    @Override
    public Optional<ProfilePhoto> getMinPhoto() {
        return user.getMinPhoto();
    }

    @Override
    public Optional<Photo> getPhoto() {
        return user.getPhoto();
    }

    @Override
    public Optional<Duration> getMessageAutoDeleteDuration() {
        return user.getMessageAutoDeleteDuration();
    }

    @Override
    public Optional<Integer> getPinnedMessageId() {
        return user.getPinnedMessageId();
    }

    @Override
    public Mono<AuxiliaryMessages> getPinnedMessage(EntityRetrievalStrategy strategy) {
        return user.getPinnedMessage(strategy);
    }

    @Override
    public Optional<PeerNotifySettings> getNotifySettings() {
        return user.getNotifySettings();
    }

    @Override
    public Optional<String> getAbout() {
        return user.getAbout();
    }

    @Override
    public Optional<Integer> getFolderId() {
        return user.getFolderId();
    }

    @Override
    public Optional<String> getThemeEmoticon() {
        return user.getThemeEmoticon();
    }

    @Override
    public String toString() {
        return "PrivateChat{" +
                "user=" + user +
                ", selfUser=" + selfUser +
                '}';
    }
}
