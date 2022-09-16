package telegram4j.core.event.dispatcher;

import reactor.core.publisher.Flux;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.inline.CallbackQueryEvent;
import telegram4j.core.event.domain.inline.InlineCallbackQueryEvent;
import telegram4j.core.event.domain.inline.InlineQueryEvent;
import telegram4j.core.object.GeoPoint;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.InlineMessageId;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseGeoPoint;
import telegram4j.tl.UpdateBotCallbackQuery;
import telegram4j.tl.UpdateBotInlineQuery;
import telegram4j.tl.UpdateInlineBotCallbackQuery;

import java.util.Objects;
import java.util.Optional;

class BotUpdatesHandlers {

    // Update handler
    // =====================

    static Flux<InlineCallbackQueryEvent> handleUpdateInlineBotCallbackQuery(
            StatefulUpdateContext<UpdateInlineBotCallbackQuery, Void> context) {
        MTProtoTelegramClient client = context.getClient();
        UpdateInlineBotCallbackQuery upd = context.getUpdate();

        User user = Objects.requireNonNull(context.getUsers().get(upd.userId()));
        InlineMessageId msgId = InlineMessageId.from(upd.msgId());

        return Flux.just(new InlineCallbackQueryEvent(client, upd.queryId(),
                user, upd.chatInstance(), upd.data(),
                upd.gameShortName(), msgId));
    }

    static Flux<CallbackQueryEvent> handleUpdateBotCallbackQuery(StatefulUpdateContext<UpdateBotCallbackQuery, Void> context) {
        MTProtoTelegramClient client = context.getClient();
        UpdateBotCallbackQuery upd = context.getUpdate();

        User user = Objects.requireNonNull(context.getUsers().get(upd.userId()));
        Chat chat = context.getChatEntity(upd.peer()).orElseThrow();

        return Flux.just(new CallbackQueryEvent(client, upd.queryId(),
                user, chat, upd.msgId(),
                upd.chatInstance(), upd.data(),
                upd.gameShortName()));
    }

    static Flux<InlineQueryEvent> handleUpdateBotInlineQuery(StatefulUpdateContext<UpdateBotInlineQuery, Void> context) {
        MTProtoTelegramClient client = context.getClient();
        UpdateBotInlineQuery upd = context.getUpdate();

        User user = Objects.requireNonNull(context.getUsers().get(upd.userId()));
        GeoPoint geo = Optional.ofNullable(TlEntityUtil.unmapEmpty(upd.geo(), BaseGeoPoint.class))
                .map(d -> new GeoPoint(client, d))
                .orElse(null);

        return Flux.just(new InlineQueryEvent(client, upd.queryId(), user,
                upd.query(), geo, upd.peerType(), upd.offset()));
    }
}
