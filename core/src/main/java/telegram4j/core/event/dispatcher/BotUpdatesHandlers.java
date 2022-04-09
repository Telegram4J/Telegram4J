package telegram4j.core.event.dispatcher;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.inline.CallbackQueryEvent;
import telegram4j.core.event.domain.inline.InlineCallbackQueryEvent;
import telegram4j.core.event.domain.inline.InlineQueryEvent;
import telegram4j.core.object.GeoPoint;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.util.EntityFactory;
import telegram4j.core.util.InlineMessageId;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;

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
        var chat = Optional.of(context.getUpdate().peer())
                .map(p -> {
                    switch (p.identifier()) {
                        case PeerUser.ID: return new PrivateChat(client, user, null);
                        case PeerChat.ID:
                        case PeerChannel.ID:
                            long rawId = TlEntityUtil.getRawPeerId(p);
                            return EntityFactory.createChat(client, context.getChats().get(rawId), null);
                        default: throw new IllegalStateException("Unknown peer type: " + p);
                    }
                })
                .orElseThrow();
        ByteBuf data = Optional.ofNullable(context.getUpdate().data())
                .map(Unpooled::wrappedBuffer)
                .map(ByteBuf::asReadOnly)
                .map(Unpooled::unreleasableBuffer)
                .orElse(null);

        return Flux.just(new CallbackQueryEvent(client, context.getUpdate().queryId(),
                user, chat, context.getUpdate().msgId(),
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
