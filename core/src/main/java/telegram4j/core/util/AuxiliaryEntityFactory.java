package telegram4j.core.util;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryChannelMessages;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.auxiliary.AuxiliaryMessagesSlice;
import telegram4j.core.auxiliary.AuxiliarySendAs;
import telegram4j.core.object.Message;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseMessageFields;
import telegram4j.tl.PeerChannel;
import telegram4j.tl.PeerChat;
import telegram4j.tl.PeerUser;
import telegram4j.tl.channels.SendAsPeers;
import telegram4j.tl.messages.BaseMessages;
import telegram4j.tl.messages.ChannelMessages;
import telegram4j.tl.messages.Messages;
import telegram4j.tl.messages.MessagesSlice;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AuxiliaryEntityFactory {

    private AuxiliaryEntityFactory() {}

    public static AuxiliaryMessages createMessages(MTProtoTelegramClient client, Messages data) {
        switch (data.identifier()) {
            case ChannelMessages.ID: {
                ChannelMessages data0 = (ChannelMessages) data;

                var usersMap = data0.users().stream()
                        .map(d -> EntityFactory.createUser(client, d))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));
                User selfUser = usersMap.get(client.getSelfId().asLong());
                var chatsMap = data0.chats().stream()
                        .map(d -> EntityFactory.createChat(client, d, selfUser))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));
                var messages = data0.messages().stream()
                        .map(m -> TlEntityUtil.unmapEmpty(m, BaseMessageFields.class))
                        .filter(Objects::nonNull)
                        .map(d -> createMessage(client, d, chatsMap, usersMap))
                        .collect(Collectors.toUnmodifiableList());
                var chats = List.copyOf(chatsMap.values());
                var users = List.copyOf(usersMap.values());

                return new AuxiliaryChannelMessages(client, data0.inexact(), data0.pts(),
                        data0.count(), data0.offsetIdOffset(), messages, chats, users);
            }
            case MessagesSlice.ID: {
                MessagesSlice data0 = (MessagesSlice) data;

                var usersMap = data0.users().stream()
                        .map(d -> EntityFactory.createUser(client, d))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));
                User selfUser = usersMap.get(client.getSelfId().asLong());
                var chatsMap = data0.chats().stream()
                        .map(d -> EntityFactory.createChat(client, d, selfUser))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));
                var messages = data0.messages().stream()
                        .map(m -> TlEntityUtil.unmapEmpty(m, BaseMessageFields.class))
                        .filter(Objects::nonNull)
                        .map(d -> createMessage(client, d, chatsMap, usersMap))
                        .collect(Collectors.toUnmodifiableList());
                var chats = List.copyOf(chatsMap.values());
                var users = List.copyOf(usersMap.values());

                return new AuxiliaryMessagesSlice(client, messages, chats, users,
                        data0.inexact(), data0.count(), data0.nextRate(),
                        data0.offsetIdOffset());
            }
            case BaseMessages.ID: {
                BaseMessages data0 = (BaseMessages) data;

                var usersMap = data0.users().stream()
                        .map(d -> EntityFactory.createUser(client, d))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));
                User selfUser = usersMap.get(client.getSelfId().asLong());
                var chatsMap = data0.chats().stream()
                        .map(d -> EntityFactory.createChat(client, d, selfUser))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));

                var messages = data0.messages().stream()
                        .map(m -> TlEntityUtil.unmapEmpty(m, BaseMessageFields.class))
                        .filter(Objects::nonNull)
                        .map(d -> createMessage(client, d, chatsMap, usersMap))
                        .collect(Collectors.toUnmodifiableList());
                var chats = List.copyOf(chatsMap.values());
                var users = List.copyOf(usersMap.values());

                return new AuxiliaryMessages(client, messages, chats, users);
            }
            default:
                throw new IllegalArgumentException("Unknown messages type: " + data);
        }
    }

    public static AuxiliarySendAs createSendAs(MTProtoTelegramClient client, SendAsPeers data) {
        var peerIds = data.peers().stream()
                .map(Id::of)
                .collect(Collectors.toUnmodifiableSet());

        var users = data.users().stream()
                .map(u -> EntityFactory.createUser(client, u))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());

        var selfUser = users.stream()
                .filter(u -> u.getId().equals(client.getSelfId()))
                .findFirst()
                .orElse(null);

        var chats = data.chats().stream()
                .map(c -> EntityFactory.createChat(client, c, selfUser))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableList());

        return new AuxiliarySendAs(client, peerIds, users, chats);
    }

    private static Message createMessage(MTProtoTelegramClient client, BaseMessageFields message,
                                         Map<Long, Chat> chatsMap, Map<Long, User> usersMap) {
        long peerId = TlEntityUtil.getRawPeerId(message.peerId());
        Id chatId;
        switch (message.peerId().identifier()) {
            case PeerChat.ID:
            case PeerChannel.ID:
                chatId = chatsMap.get(peerId).getId();
                break;
            case PeerUser.ID:
                chatId = usersMap.get(peerId).getId();
                break;
            default:
                throw new IllegalStateException();
        }

        return EntityFactory.createMessage(client, message, chatId);
    }
}
