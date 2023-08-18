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
package telegram4j.core.event.domain.message;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.Event;

/**
 * Subtype of message related events.
 *
 * <ul>
 *     <li>{@link SendMessageEvent}: a new ordinary or scheduled message in the chat/channel/user was sent.</li>
 *     <li>{@link EditMessageEvent}: an message was updated, e.g., updated text, added/removed reactions.</li>
 *     <li>{@link DeleteMessagesEvent}: a message or batch of ordinal/scheduled messages was deleted.</li>
 *     <li>{@link UpdatePinnedMessagesEvent}: a message or batch of messages was pinned/unpinned.</li>
 * </ul>
 */
public abstract sealed class MessageEvent extends Event
        permits DeleteMessagesEvent, EditMessageEvent, MessagePollResultsEvent,
                MessagePollVoteEvent, SendMessageEvent, UpdatePinnedMessagesEvent {

    protected MessageEvent(MTProtoTelegramClient client) {
        super(client);
    }
}
