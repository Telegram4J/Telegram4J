package telegram4j.core.util;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryChannelMessages;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.object.Id;
import telegram4j.core.object.chat.Chat;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BaseMessageFields;
import telegram4j.tl.messages.BaseMessages;
import telegram4j.tl.messages.ChannelMessages;
import telegram4j.tl.messages.Messages;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class AuxiliaryEntityFactory {

    private AuxiliaryEntityFactory() {}

    public static AuxiliaryMessages createMessages(MTProtoTelegramClient client, Messages data) {
        switch (data.identifier()) {
            case ChannelMessages.ID: {
                ChannelMessages data0 = (ChannelMessages) data;

                var chatsMap = data0.chats().stream()
                        .map(d -> EntityFactory.createChat(client, d))
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));
                var usersMap = data0.users().stream()
                        .map(d -> EntityFactory.createUser(client, d))
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));

                var messages = data0.messages().stream()
                        .map(m -> (BaseMessageFields) m)
                        .map(d -> {
                            long peerId = TlEntityUtil.getRawPeerId(d.peerId());
                            Id id = Optional.ofNullable(chatsMap.get(peerId))
                                    .map(Chat::getId)
                                    .orElseGet(() -> usersMap.get(peerId).getId());

                            return EntityFactory.createMessage(client, d, id);
                        })
                        .collect(Collectors.toList());
                var chats = List.copyOf(chatsMap.values());
                var users = List.copyOf(usersMap.values());

                return new AuxiliaryChannelMessages(client, data0.inexact(), data0.pts(),
                        data0.count(), data0.offsetIdOffset(), messages, chats, users);
            }
            case BaseMessages.ID: {
                BaseMessages data0 = (BaseMessages) data;

                var chatsMap = data0.chats().stream()
                        .map(d -> EntityFactory.createChat(client, d))
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));
                var usersMap = data0.users().stream()
                        .map(d -> EntityFactory.createUser(client, d))
                        .collect(Collectors.toMap(c -> c.getId().asLong(), Function.identity()));

                var messages = data0.messages().stream()
                        .map(m -> (BaseMessageFields) m)
                        .map(d -> {
                            long peerId = TlEntityUtil.getRawPeerId(d.peerId());
                            Id id = Optional.ofNullable(chatsMap.get(peerId))
                                    .map(Chat::getId)
                                    .orElseGet(() -> usersMap.get(peerId).getId());

                            return EntityFactory.createMessage(client, d, id);
                        })
                        .collect(Collectors.toList());
                var chats = List.copyOf(chatsMap.values());
                var users = List.copyOf(usersMap.values());

                return new AuxiliaryMessages(client, messages, chats, users);
            }
            default: throw new IllegalArgumentException("Unknown messages type: " + data);
        }
    }
}
