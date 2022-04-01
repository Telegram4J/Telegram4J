package telegram4j.core.event.dispatcher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.inline.CallbackQueryEvent;
import telegram4j.core.event.domain.inline.InlineCallbackQueryEvent;
import telegram4j.core.event.domain.inline.InlineQueryEvent;
import telegram4j.core.object.GeoPoint;
import telegram4j.core.object.Id;
import telegram4j.core.object.InlineMessageId;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseGeoPoint;
import telegram4j.tl.UpdateBotCallbackQuery;
import telegram4j.tl.UpdateBotInlineQuery;
import telegram4j.tl.UpdateInlineBotCallbackQuery;

import java.util.Optional;

class BotUpdatesHandlers {

    // Update handler
    // =====================

    static Flux<InlineCallbackQueryEvent> handleUpdateInlineBotCallbackQuery(
            StatefulUpdateContext<UpdateInlineBotCallbackQuery, Void> context) {
        MTProtoTelegramClient client = context.getClient();

        var user = EntityFactory.createUser(client, context.getUsers().get(context.getUpdate().userId()));
        ByteBuf data = Optional.ofNullable(context.getUpdate().data())
                .map(Unpooled::wrappedBuffer)
                .map(ByteBuf::asReadOnly)
                .map(Unpooled::unreleasableBuffer) // no need release because it's byte array based buffer
                .orElse(null);
        InlineMessageId msgId = InlineMessageId.from(context.getUpdate().msgId());

        return Flux.just(new InlineCallbackQueryEvent(client, context.getUpdate().queryId(),
                user, context.getUpdate().chatInstance(), data,
                context.getUpdate().gameShortName(), msgId));
    }

    static Flux<CallbackQueryEvent> handleUpdateBotCallbackQuery(StatefulUpdateContext<UpdateBotCallbackQuery, Void> context) {
        MTProtoTelegramClient client = context.getClient();

        var user = EntityFactory.createUser(client, context.getUsers().get(context.getUpdate().userId()));
        Id peerId = Id.of(context.getUpdate().peer());
        ByteBuf data = Optional.ofNullable(context.getUpdate().data())
                .map(Unpooled::wrappedBuffer)
                .map(ByteBuf::asReadOnly)
                .map(Unpooled::unreleasableBuffer)
                .orElse(null);

        return Flux.just(new CallbackQueryEvent(client, context.getUpdate().queryId(),
                user, peerId, context.getUpdate().msgId(),
                context.getUpdate().chatInstance(), data,
                context.getUpdate().gameShortName()));
    }

    static Flux<InlineQueryEvent> handleUpdateBotInlineQuery(StatefulUpdateContext<UpdateBotInlineQuery, Void> context) {
        MTProtoTelegramClient client = context.getClient();

        var user = EntityFactory.createUser(client, context.getUsers().get(context.getUpdate().userId()));
        GeoPoint geo = Optional.ofNullable(TlEntityUtil.unmapEmpty(context.getUpdate().geo(), BaseGeoPoint.class))
                .map(d -> new GeoPoint(client, d))
                .orElse(null);

        return Flux.just(new InlineQueryEvent(client, context.getUpdate().queryId(),
                user, context.getUpdate().query(), geo,
                context.getUpdate().peerType(), context.getUpdate().offset()));
    }
}
