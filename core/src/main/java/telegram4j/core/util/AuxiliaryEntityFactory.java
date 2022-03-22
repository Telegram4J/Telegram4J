package telegram4j.core.util;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryChannelMessages;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.auxiliary.AuxiliaryMessagesSlice;
import telegram4j.core.object.Id;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.messages.BaseMessages;
import telegram4j.tl.messages.ChannelMessages;
import telegram4j.tl.messages.Messages;
import telegram4j.tl.messages.MessagesSlice;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AuxiliaryEntityFactory {

    private AuxiliaryEntityFactory() {}

    public static AuxiliaryMessages createMessages(MTProtoTelegramClient client, Messages data) {
        switch (data.identifier()) {
            case ChannelMessages.ID: {
                ChannelMessages data0 = (ChannelMessages) data;

                var chatsMap = data0.chats().stream()
                        .filter(TlEntityUtil::isAvailableChat)
                        .map(d -> EntityFactory.createChat(client, d, null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));
                var usersMap = data0.users().stream()
                        .filter(u -> u.identifier() == BaseUser.ID)
                        .map(d -> EntityFactory.createUser(client, d))
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));

                var messages = data0.messages().stream()
                        .filter(m -> m.identifier() != MessageEmpty.ID)
                        .map(m -> (BaseMessageFields) m)
                        .map(d -> {
                            long peerId = TlEntityUtil.getRawPeerId(d.peerId());
                            Id chatId;
                            switch (d.peerId().identifier()) {
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

                            return EntityFactory.createMessage(client, d, chatId);
                        })
                        .collect(Collectors.toUnmodifiableList());
                var chats = List.copyOf(chatsMap.values());
                var users = List.copyOf(usersMap.values());

                return new AuxiliaryChannelMessages(client, data0.inexact(), data0.pts(),
                        data0.count(), data0.offsetIdOffset(), messages, chats, users);
            }
            case MessagesSlice.ID: {
                MessagesSlice data0 = (MessagesSlice) data;

                var chatsMap = data0.chats().stream()
                        .filter(TlEntityUtil::isAvailableChat)
                        .map(d -> EntityFactory.createChat(client, d, null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));
                var usersMap = data0.users().stream()
                        .filter(u -> u.identifier() == BaseUser.ID)
                        .map(d -> EntityFactory.createUser(client, d))
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));

                var messages = data0.messages().stream()
                        .filter(m -> m.identifier() != MessageEmpty.ID)
                        .map(m -> (BaseMessageFields) m)
                        .map(d -> {
                            long peerId = TlEntityUtil.getRawPeerId(d.peerId());
                            Id chatId;
                            switch (d.peerId().identifier()) {
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

                            return EntityFactory.createMessage(client, d, chatId);
                        })
                        .collect(Collectors.toUnmodifiableList());
                var chats = List.copyOf(chatsMap.values());
                var users = List.copyOf(usersMap.values());

                return new AuxiliaryMessagesSlice(client, messages, chats, users,
                        data0.inexact(), data0.count(), data0.nextRate(),
                        data0.offsetIdOffset());
            }
            case BaseMessages.ID: {
                BaseMessages data0 = (BaseMessages) data;

                var chatsMap = data0.chats().stream()
                        .filter(TlEntityUtil::isAvailableChat)
                        .map(d -> EntityFactory.createChat(client, d, null))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));
                var usersMap = data0.users().stream()
                        .filter(u -> u.identifier() == BaseUser.ID)
                        .map(d -> EntityFactory.createUser(client, d))
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));

                var messages = data0.messages().stream()
                        .filter(m -> m.identifier() != MessageEmpty.ID)
                        .map(m -> (BaseMessageFields) m)
                        .map(d -> {
                            long peerId = TlEntityUtil.getRawPeerId(d.peerId());
                            Id chatId;
                            switch (d.peerId().identifier()) {
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

                            return EntityFactory.createMessage(client, d, chatId);
                        })
                        .collect(Collectors.toUnmodifiableList());
                var chats = List.copyOf(chatsMap.values());
                var users = List.copyOf(usersMap.values());

                return new AuxiliaryMessages(client, messages, chats, users);
            }
            default:
                throw new IllegalArgumentException("Unknown messages type: " + data);
        }
    }
}
