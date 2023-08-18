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

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.Event;
import telegram4j.core.object.chat.Chat;

/**
 * Subtype of chat/channel related events.
 *
 * <p>Chat Participant Events (Bot-Only)
 * <ul>
 *     <li>{@link ChatParticipantsUpdateEvent}: a batch event of participants updates.</li>
 * </ul>
 */
// TODO: docs for ChatParticipantAdminEvent
public abstract sealed class ChatEvent extends Event
        permits ChatParticipantUpdateEvent, ChatParticipantsUpdateEvent {

    protected ChatEvent(MTProtoTelegramClient client) {
        super(client);
    }

    public abstract Chat getChat();
}
