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
package telegram4j.core.event.domain.inline;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.Event;
import telegram4j.core.object.User;

/**
 * Subtype of bot interaction events.
 */
public abstract sealed class BotEvent extends Event
        permits CallbackEvent, InlineQueryEvent {

    protected BotEvent(MTProtoTelegramClient client) {
        super(client);
    }

    /**
     * Gets id of current query.
     *
     * @return The id of current query.
     */
    public abstract long getQueryId();

    /**
     * Gets {@link User} which starts this interaction.
     *
     * @return The {@link User} which starts this interaction.
     */
    public abstract User getUser();
}
