package telegram4j.core.internal;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.dispatcher.StatefulUpdateContext;
import telegram4j.core.object.MentionablePeer;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.object.chat.UnresolvedPeerException;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.BitFlag;
import telegram4j.core.util.Id;
import telegram4j.core.util.ImmutableEnumSet;
import telegram4j.tl.BaseMessageFields;

import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class MappingUtil {
    private MappingUtil() {}

    public static final EntityRetrievalStrategy IDENTITY_RETRIEVER = client -> client;

    public static <E extends Enum<E> & BitFlag> int getMaskValue(Iterable<E> values) {
        if (values.getClass() == ImmutableEnumSet.class)
            return ((ImmutableEnumSet<E>) values).getValue();
        return StreamSupport.stream(values.spliterator(), false)
                .map(E::mask)
                .reduce(0, (l, r) -> l | r);
    }

    public static <T> Mono<T> unresolvedPeer(Id peerId) {
        return Mono.error(() -> new UnresolvedPeerException(peerId));
    }

    public static Optional<MentionablePeer> getAuthor(BaseMessageFields message, @Nullable Chat chat,
                                                      MTProtoTelegramClient client,
                                                      Map<Id, Chat> chatsMap, Map<Id, User> usersMap) {
        return Optional.ofNullable(message.fromId())
                .map(p -> {
                    Id id = Id.of(p);
                    switch (id.getType()) {
                        case CHANNEL: return (MentionablePeer) chatsMap.get(id);
                        case USER: return usersMap.get(id);
                        default: throw new IllegalStateException();
                    }
                })
                // fromId is often not set if the message was sent to the DM, so we will have to process it for convenience
                .or(() -> Optional.ofNullable(chat)
                        .map(c -> {
                            switch (c.getType()) {
                                case PRIVATE:
                                    PrivateChat pc = (PrivateChat) c;
                                    return message.out() ? pc.getSelfUser().orElse(null) : pc.getUser();
                                case CHANNEL: return usersMap.get(client.getChannelBotId());
                                case SUPERGROUP: return usersMap.get(client.getGroupAnonymousBotId());
                                default: return null;
                            }
                        }));
    }

    public static Optional<MentionablePeer> getAuthor(StatefulUpdateContext<?, ?> ctx, BaseMessageFields message,
                                                      @Nullable Chat chat) {
        return getAuthor(message, chat, ctx.getClient(), ctx.getChats(), ctx.getUsers());
    }
}
