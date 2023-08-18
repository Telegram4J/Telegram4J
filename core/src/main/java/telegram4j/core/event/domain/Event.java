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
package telegram4j.core.event.domain;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.chat.ChatEvent;
import telegram4j.core.event.domain.inline.BotEvent;
import telegram4j.core.event.domain.message.MessageEvent;

import java.util.Objects;

/** General interface of Telegram API events. */
public abstract sealed class Event
        permits ChatEvent, BotEvent, MessageEvent {

    protected final MTProtoTelegramClient client;

    protected Event(MTProtoTelegramClient client) {
        this.client = Objects.requireNonNull(client);
    }

    /**
     * Gets client that applied this event.
     *
     * @return The client applying this event.
     */
    public final MTProtoTelegramClient getClient() {
        return client;
    }
}
