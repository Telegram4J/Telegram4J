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

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.Id;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AuxiliaryChannelMessages extends AuxiliaryMessages {
    private final boolean inexact;
    private final int pts;
    private final int count;
    @Nullable
    private final Integer offsetId;

    public AuxiliaryChannelMessages(MTProtoTelegramClient client,
                                    boolean inexact, int pts, int count, @Nullable Integer offsetId,
                                    List<Message> messages, Map<Id, Chat> chats, Map<Id, User> users) {
        super(client, messages, chats, users);
        this.inexact = inexact;
        this.pts = pts;
        this.count = count;
        this.offsetId = offsetId;
    }

    public boolean isInexact() {
        return inexact;
    }

    public int getPts() {
        return pts;
    }

    public int getCount() {
        return count;
    }

    public Optional<Integer> getOffsetId() {
        return Optional.ofNullable(offsetId);
    }

    @Override
    public String toString() {
        return "AuxiliaryChannelMessages{" +
                "inexact=" + inexact +
                ", pts=" + pts +
                ", count=" + count +
                ", offsetId=" + offsetId +
                "} " + super.toString();
    }
}
