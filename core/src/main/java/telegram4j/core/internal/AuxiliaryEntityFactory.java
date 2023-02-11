package telegram4j.core.internal;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryChannelMessages;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.auxiliary.AuxiliaryMessagesSlice;
import telegram4j.core.auxiliary.AuxiliaryStickerSet;
import telegram4j.core.object.Message;
import telegram4j.core.object.StickerSet;
import telegram4j.core.object.User;
import telegram4j.core.object.*;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.Id;
import telegram4j.mtproto.file.Context;
import telegram4j.tl.*;
import telegram4j.tl.messages.*;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AuxiliaryEntityFactory {

    private AuxiliaryEntityFactory() {}

    public static AuxiliaryStickerSet createStickerSet(MTProtoTelegramClient client, BaseStickerSet data) {
        var stickerSet = new StickerSet(client, data.set());
        var ctx = Context.createStickerSetContext(ImmutableInputStickerSetID.of(
                stickerSet.getId(), stickerSet.getAccessHash()));
        var stickers = data.documents().stream()
                .flatMap(d -> d instanceof BaseDocument b
                        ? Stream.of((Sticker) EntityFactory.createDocument(client, b, ctx))
                        : Stream.empty())
                .collect(Collectors.toMap(s -> s.getId().orElseThrow(), Function.identity()));

        return new AuxiliaryStickerSet(client, stickerSet, stickers, data.packs());
    }

    public static AuxiliaryMessages createMessages(MTProtoTelegramClient client, Messages data) {
        switch (data.identifier()) {
            case ChannelMessages.ID -> {
                var data0 = (ChannelMessages) data;

                var users = data0.users().stream()
                        .flatMap(d -> Stream.ofNullable(EntityFactory.createUser(client, d)))
                        .collect(Collectors.toUnmodifiableMap(PeerEntity::getId, Function.identity()));
                var chats = data0.chats().stream()
                        .flatMap(d -> Stream.ofNullable(EntityFactory.createChat(client, d, null)))
                        .collect(Collectors.toUnmodifiableMap(PeerEntity::getId, Function.identity()));
                var messages = data0.messages().stream()
                        .flatMap(d -> createMessage(client, d, chats, users))
                        .collect(Collectors.toUnmodifiableList());

                return new AuxiliaryChannelMessages(client, data0.inexact(), data0.pts(),
                        data0.count(), data0.offsetIdOffset(), messages, chats, users);
            }
            case MessagesSlice.ID -> {
                var data0 = (MessagesSlice) data;

                var users = data0.users().stream()
                        .flatMap(d -> Stream.ofNullable(EntityFactory.createUser(client, d)))
                        .collect(Collectors.toUnmodifiableMap(PeerEntity::getId, Function.identity()));
                var chats = data0.chats().stream()
                        .flatMap(d -> Stream.ofNullable(EntityFactory.createChat(client, d, null)))
                        .collect(Collectors.toUnmodifiableMap(PeerEntity::getId, Function.identity()));
                var messages = data0.messages().stream()
                        .flatMap(d -> createMessage(client, d, chats, users))
                        .collect(Collectors.toUnmodifiableList());

                return new AuxiliaryMessagesSlice(client, messages, chats, users,
                        data0.inexact(), data0.count(), data0.nextRate(),
                        data0.offsetIdOffset());
            }
            case BaseMessages.ID -> {
                var data0 = (BaseMessages) data;

                var users = data0.users().stream()
                        .flatMap(d -> Stream.ofNullable(EntityFactory.createUser(client, d)))
                        .collect(Collectors.toUnmodifiableMap(PeerEntity::getId, Function.identity()));
                var chats = data0.chats().stream()
                        .flatMap(d -> Stream.ofNullable(EntityFactory.createChat(client, d, null)))
                        .collect(Collectors.toUnmodifiableMap(PeerEntity::getId, Function.identity()));
                var messages = data0.messages().stream()
                        .flatMap(d -> createMessage(client, d, chats, users))
                        .collect(Collectors.toUnmodifiableList());

                return new AuxiliaryMessages(client, messages, chats, users);
            }
            default -> throw new IllegalArgumentException("Unknown Messages type: " + data);
        }
    }

    private static Stream<Message> createMessage(MTProtoTelegramClient client, telegram4j.tl.Message data,
                                                 Map<Id, Chat> chatsMap, Map<Id, User> usersMap) {
        Peer peerId;
        if (data instanceof BaseMessage b) {
            peerId = b.peerId();
        } else if (data instanceof MessageService s) {
            peerId = s.peerId();
        } else {
            return Stream.empty();
        }

        // here we're getting id of chat with access_hash for Message methods
        Id chatId = Id.of(peerId);
        PeerEntity peer = switch (chatId.getType()) {
            case USER -> usersMap.get(chatId);
            case CHAT, CHANNEL -> chatsMap.get(chatId);
        };

        chatId = peer != null ? peer.getId() : chatId;
        return Stream.of(EntityFactory.createMessage(client, data, chatId));
    }
}
