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
package telegram4j.core.event.domain.chat;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.object.chat.GroupChat;
import telegram4j.core.object.chat.GroupChatPeer;

import java.util.List;
import java.util.Optional;

/** Event of batch modification of group chat participants. */
public final class ChatParticipantsUpdateEvent extends ChatEvent {

    private final GroupChatPeer chat;
    @Nullable
    private final ChatParticipant selfParticipant;
    @Nullable
    private final Integer version;
    @Nullable
    private final List<ChatParticipant> participants;

    public ChatParticipantsUpdateEvent(MTProtoTelegramClient client, GroupChatPeer chat,
                                       @Nullable ChatParticipant selfParticipant, @Nullable Integer version,
                                       @Nullable List<ChatParticipant> participants) {
        super(client);
        this.chat = chat;
        this.selfParticipant = selfParticipant;
        this.version = version;
        this.participants = participants;
    }

    /**
     * Gets whether access to list of participants is forbidden.
     *
     * @return {@code true} if access to list of participants is forbidden.
     */
    public boolean isForbidden() {
        return version == null;
    }

    /**
     * Gets group chat where participants were updated.
     *
     * @return The {@link GroupChatPeer} where participants were updated.
     */
    @Override
    public GroupChatPeer getChat() {
        return chat;
    }

    /**
     * Gets self participant if {@link #isForbidden()} and <i>current</i> user is a chat participant.
     *
     * @return The {@link ChatParticipant} of self user, if present.
     */
    public Optional<ChatParticipant> getSelfParticipant() {
        return Optional.ofNullable(selfParticipant);
    }

    /**
     * Gets current version of group chat participants if {@link #isForbidden()} is {@code false}.
     *
     * @return The current version of group chat participants, if present.
     */
    public Optional<Integer> getVersion() {
        return Optional.ofNullable(version);
    }

    /**
     * Gets list of {@link ChatParticipant}s of this group chat, if {@link #isForbidden()} is {@code false}.
     *
     * @return The {@link List} of {@link ChatParticipant}s of this group chat, if present.
     */
    public Optional<List<ChatParticipant>> getParticipants() {
        return Optional.ofNullable(participants);
    }

    @Override
    public String toString() {
        return "ChatParticipantsUpdateEvent{" +
                "chat=" + chat +
                ", selfParticipant=" + selfParticipant +
                ", version=" + version +
                ", participants=" + participants +
                '}';
    }
}
