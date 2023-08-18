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
package telegram4j.mtproto.store.object;

import reactor.util.annotation.Nullable;
import telegram4j.tl.BaseUser;
import telegram4j.tl.ChatParticipant;

import java.util.Objects;
import java.util.Optional;

public class ResolvedChatParticipant {
    private final ChatParticipant participant;
    @Nullable
    private final BaseUser user;

    public ResolvedChatParticipant(ChatParticipant participant, @Nullable BaseUser user) {
        this.participant = Objects.requireNonNull(participant);
        this.user = user;
    }

    public ChatParticipant getParticipant() {
        return participant;
    }

    public Optional<BaseUser> getUser() {
        return Optional.ofNullable(user);
    }

    @Override
    public String toString() {
        return "ResolvedChatParticipant{" +
                "participant=" + participant +
                ", user=" + user +
                '}';
    }
}
