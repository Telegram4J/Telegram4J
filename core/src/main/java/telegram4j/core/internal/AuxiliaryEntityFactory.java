package telegram4j.core.internal;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryChannelMessages;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.auxiliary.AuxiliaryMessagesSlice;
import telegram4j.core.object.Message;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.Id;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseMessageFields;
import telegram4j.tl.messages.BaseMessages;
import telegram4j.tl.messages.ChannelMessages;
import telegram4j.tl.messages.Messages;
import telegram4j.tl.messages.MessagesSlice;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AuxiliaryEntityFactory {

    private AuxiliaryEntityFactory() {}

    public static AuxiliaryMessages createMessages(MTProtoTelegramClient client, Messages data) {
        switch (data.identifier()) {
            case ChannelMessages.ID: {
                ChannelMessages data0 = (ChannelMessages) data;

                var users = data0.users().stream()
                        .map(d -> EntityFactory.createUser(client, d))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableMap(PeerEntity::getId, Function.identity()));
                var chats = data0.chats().stream()
                        .map(d -> EntityFactory.createChat(client, d, null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableMap(PeerEntity::getId, Function.identity()));
                var messages = data0.messages().stream()
                        .map(m -> TlEntityUtil.unmapEmpty(m, BaseMessageFields.class))
                        .filter(Objects::nonNull)
                        .map(d -> createMessage(client, d, chats, users))
                        .collect(Collectors.toUnmodifiableList());

                return new AuxiliaryChannelMessages(client, data0.inexact(), data0.pts(),
                        data0.count(), data0.offsetIdOffset(), messages, chats, users);
            }
            case MessagesSlice.ID: {
                MessagesSlice data0 = (MessagesSlice) data;

                var users = data0.users().stream()
                        .map(d -> EntityFactory.createUser(client, d))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableMap(PeerEntity::getId, Function.identity()));
                var chats = data0.chats().stream()
                        .map(d -> EntityFactory.createChat(client, d, null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableMap(PeerEntity::getId, Function.identity()));
                var messages = data0.messages().stream()
                        .map(m -> TlEntityUtil.unmapEmpty(m, BaseMessageFields.class))
                        .filter(Objects::nonNull)
                        .map(d -> createMessage(client, d, chats, users))
                        .collect(Collectors.toUnmodifiableList());

                return new AuxiliaryMessagesSlice(client, messages, chats, users,
                        data0.inexact(), data0.count(), data0.nextRate(),
                        data0.offsetIdOffset());
            }
            case BaseMessages.ID: {
                BaseMessages data0 = (BaseMessages) data;

                var users = data0.users().stream()
                        .map(d -> EntityFactory.createUser(client, d))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableMap(PeerEntity::getId, Function.identity()));
                var chats = data0.chats().stream()
                        .map(d -> EntityFactory.createChat(client, d, null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toUnmodifiableMap(PeerEntity::getId, Function.identity()));
                var messages = data0.messages().stream()
                        .map(m -> TlEntityUtil.unmapEmpty(m, BaseMessageFields.class))
                        .filter(Objects::nonNull)
                        .map(d -> createMessage(client, d, chats, users))
                        .collect(Collectors.toUnmodifiableList());

                return new AuxiliaryMessages(client, messages, chats, users);
            }
            default:
                throw new IllegalArgumentException("Unknown Messages type: " + data);
        }
    }

    private static Message createMessage(MTProtoTelegramClient client, BaseMessageFields message,
                                         Map<Id, Chat> chatsMap, Map<Id, User> usersMap) {
        // here we're getting id of chat with access_hash for Message methods
        Id chatId = Id.of(message.peerId());
        PeerEntity peer;
        switch (chatId.getType()) {
            case USER:
                peer = usersMap.get(chatId);
                break;
            case CHAT:
            case CHANNEL:
                peer = chatsMap.get(chatId);
                break;
            default: throw new IllegalStateException();
        }

        chatId = peer != null ? peer.getId() : chatId;
        return EntityFactory.createMessage(client, message, chatId);
    }
}
