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
package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.inline.CallbackQueryEvent;
import telegram4j.core.event.domain.inline.InlineCallbackQueryEvent;
import telegram4j.core.event.domain.inline.InlineQueryEvent;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.media.GeoPoint;
import telegram4j.core.util.Id;
import telegram4j.core.util.InlineMessageId;
import telegram4j.tl.BaseGeoPoint;
import telegram4j.tl.UpdateBotCallbackQuery;
import telegram4j.tl.UpdateBotInlineQuery;
import telegram4j.tl.UpdateInlineBotCallbackQuery;

import java.util.Objects;

class BotUpdatesHandlers {

    // Update handler
    // =====================

    static Flux<InlineCallbackQueryEvent> handleUpdateInlineBotCallbackQuery(
            StatefulUpdateContext<UpdateInlineBotCallbackQuery, Void> context) {
        MTProtoTelegramClient client = context.getClient();
        UpdateInlineBotCallbackQuery upd = context.getUpdate();

        User user = Objects.requireNonNull(context.getUsers().get(Id.ofUser(upd.userId())));
        InlineMessageId msgId = InlineMessageId.from(upd.msgId());

        return Flux.just(new InlineCallbackQueryEvent(client, upd.queryId(),
                user, upd.chatInstance(), upd.data(),
                upd.gameShortName(), msgId));
    }

    static Flux<CallbackQueryEvent> handleUpdateBotCallbackQuery(StatefulUpdateContext<UpdateBotCallbackQuery, Void> context) {
        MTProtoTelegramClient client = context.getClient();
        UpdateBotCallbackQuery upd = context.getUpdate();

        User user = Objects.requireNonNull(context.getUsers().get(Id.ofUser(upd.userId())));
        Chat chat = context.getChatEntity(Id.of(upd.peer())).orElseThrow();

        return Flux.just(new CallbackQueryEvent(client, upd.queryId(),
                user, chat, upd.msgId(),
                upd.chatInstance(), upd.data(),
                upd.gameShortName()));
    }

    static Flux<InlineQueryEvent> handleUpdateBotInlineQuery(StatefulUpdateContext<UpdateBotInlineQuery, Void> context) {
        MTProtoTelegramClient client = context.getClient();
        UpdateBotInlineQuery upd = context.getUpdate();

        User user = Objects.requireNonNull(context.getUsers().get(Id.ofUser(upd.userId())));
        GeoPoint geo = upd.geo() instanceof BaseGeoPoint b ? new GeoPoint(b) : null;

        return Flux.just(new InlineQueryEvent(client, upd.queryId(), user,
                upd.query(), geo, upd.peerType(), upd.offset()));
    }
}
