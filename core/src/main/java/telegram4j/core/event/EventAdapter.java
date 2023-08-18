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
package telegram4j.core.event;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.Event;
import telegram4j.core.event.domain.chat.ChatEvent;
import telegram4j.core.event.domain.chat.ChatParticipantUpdateEvent;
import telegram4j.core.event.domain.chat.ChatParticipantsUpdateEvent;
import telegram4j.core.event.domain.inline.*;
import telegram4j.core.event.domain.message.*;

import java.util.ArrayList;

public abstract class EventAdapter {
    // region ChatEvent

    public Publisher<?> onChatEvent(ChatEvent event) {
        return Mono.empty();
    }

    public Publisher<?> onChatParticipantUpdate(ChatParticipantUpdateEvent event) {
        return Mono.empty();
    }

    public Publisher<?> onChatParticipantsUpdate(ChatParticipantsUpdateEvent event) {
        return Mono.empty();
    }

    // endregion
    // region BotEvent

    public Publisher<?> onBotEvent(BotEvent event) {
        return Mono.empty();
    }

    public Publisher<?> onCallbackEvent(CallbackEvent event) {
        return Mono.empty();
    }

    public Publisher<?> onCallbackQuery(CallbackQueryEvent event) {
        return Mono.empty();
    }

    public Publisher<?> onInlineCallbackQuery(InlineCallbackQueryEvent event) {
        return Mono.empty();
    }

    public Publisher<?> onInlineQuery(InlineQueryEvent event) {
        return Mono.empty();
    }

    // endregion
    // region MessageEvent

    public Publisher<?> onMessageEvent(MessageEvent event) {
        return Mono.empty();
    }

    public Publisher<?> onDeleteMessages(DeleteMessagesEvent event) {
        return Mono.empty();
    }

    public Publisher<?> onEditMessage(EditMessageEvent event) {
        return Mono.empty();
    }

    public Publisher<?> onMessagePollResults(MessagePollResultsEvent event) {
        return Mono.empty();
    }

    public Publisher<?> onMessagePollVote(MessagePollVoteEvent event) {
        return Mono.empty();
    }

    public Publisher<?> onSendMessage(SendMessageEvent event) {
        return Mono.empty();
    }

    public Publisher<?> onUpdatePinnedMessages(UpdatePinnedMessagesEvent event) {
        return Mono.empty();
    }

    // endregion

    public Publisher<?> hookOnEvent(Event event) {
        var compatible = new ArrayList<Publisher<?>>();
        if (event instanceof ChatEvent e) compatible.add(onChatEvent(e));
        if (event instanceof ChatParticipantUpdateEvent e) compatible.add(onChatParticipantUpdate(e));
        if (event instanceof ChatParticipantsUpdateEvent e) compatible.add(onChatParticipantsUpdate(e));
        if (event instanceof BotEvent e) compatible.add(onBotEvent(e));
        if (event instanceof CallbackEvent e) compatible.add(onCallbackEvent(e));
        if (event instanceof CallbackQueryEvent e) compatible.add(onCallbackQuery(e));
        if (event instanceof InlineCallbackQueryEvent e) compatible.add(onInlineCallbackQuery(e));
        if (event instanceof InlineQueryEvent e) compatible.add(onInlineQuery(e));
        if (event instanceof MessageEvent e) compatible.add(onMessageEvent(e));
        if (event instanceof DeleteMessagesEvent e) compatible.add(onDeleteMessages(e));
        if (event instanceof EditMessageEvent e) compatible.add(onEditMessage(e));
        if (event instanceof MessagePollResultsEvent e) compatible.add(onMessagePollResults(e));
        if (event instanceof MessagePollVoteEvent e) compatible.add(onMessagePollVote(e));
        if (event instanceof SendMessageEvent e) compatible.add(onSendMessage(e));
        if (event instanceof UpdatePinnedMessagesEvent e) compatible.add(onUpdatePinnedMessages(e));
        return Mono.whenDelayError(compatible);
    }
}
