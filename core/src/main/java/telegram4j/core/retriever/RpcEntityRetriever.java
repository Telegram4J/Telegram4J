package telegram4j.core.retriever;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.function.TupleUtils;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.internal.AuxiliaryEntityFactory;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.object.MentionablePeer;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Channel;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.util.Id;
import telegram4j.core.util.PaginationSupport;
import telegram4j.core.util.PeerId;
import telegram4j.mtproto.service.ServiceHolder;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.channels.BaseChannelParticipants;
import telegram4j.tl.messages.MessagesNotModified;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Implementation of {@code EntityRetriever} which uses Telegram RPC API. */
public class RpcEntityRetriever implements EntityRetriever {

    private final MTProtoTelegramClient client;
    private final ServiceHolder serviceHolder;
    private final boolean retrieveSelfUserForDMs;

    public RpcEntityRetriever(MTProtoTelegramClient client) {
        this(client, true);
    }

    public RpcEntityRetriever(MTProtoTelegramClient client, boolean retrieveSelfUserForDMs) {
        this(client, client.getServiceHolder(), retrieveSelfUserForDMs);
    }

    private RpcEntityRetriever(MTProtoTelegramClient client, ServiceHolder serviceHolder, boolean retrieveSelfUserForDMs) {
        this.client = client;
        this.serviceHolder = serviceHolder;
        this.retrieveSelfUserForDMs = retrieveSelfUserForDMs;
    }

    public RpcEntityRetriever withRetrieveSelfUserForDMs(boolean state) {
        if (retrieveSelfUserForDMs == state) return this;
        return new RpcEntityRetriever(client, serviceHolder, state);
    }

    @Override
    public Mono<PeerEntity> resolvePeer(PeerId peerId) {
        Mono<PeerEntity> resolveById = Mono.justOrEmpty(peerId.asId())
                .flatMap(id -> switch (id.getType()) {
                    case CHAT, CHANNEL -> getChatMinById(id);
                    case USER -> getUserMinById(id);
                });

        return Mono.justOrEmpty(peerId.asUsername())
                .flatMap(serviceHolder.getUserService()::resolveUsername)
                .flatMap(p -> switch (p.peer().identifier()) {
                    case PeerChannel.ID -> Mono.justOrEmpty(EntityFactory.createChat(client, p.chats().get(0), null));
                    case PeerUser.ID -> Mono.justOrEmpty(EntityFactory.createUser(client, p.users().get(0)));
                    default -> Mono.error(new IllegalStateException("Unknown Peer type: " + p.peer()));
                })
                .switchIfEmpty(resolveById);
    }

    @Override
    public Mono<User> getUserMinById(Id userId) {
        if (userId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.USER + ", but given: " + userId.getType()));
        }

        return client.asInputUser(userId)
                .flatMap(serviceHolder.getUserService()::getUser)
                .mapNotNull(u -> EntityFactory.createUser(client, u));
    }

    @Override
    public Mono<User> getUserFullById(Id userId) {
        if (userId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.USER + ", but given: " + userId.getType()));
        }

        return client.asInputUser(userId)
                .flatMap(serviceHolder.getUserService()::getFullUser)
                .mapNotNull(u -> EntityFactory.createUser(client, u));
    }

    @Override
    public Mono<Chat> getChatMinById(Id chatId) {
        return Mono.defer(() -> switch (chatId.getType()) {
            case CHAT -> serviceHolder.getChatService().getChat(chatId.asLong())
                    .mapNotNull(c -> EntityFactory.createChat(client, c, null));
            case CHANNEL -> client.asInputChannel(chatId)
                    .flatMap(serviceHolder.getChatService()::getChannel)
                    .mapNotNull(c -> EntityFactory.createChat(client, c, null));
            case USER -> client.asInputUser(chatId)
                    .flatMap(serviceHolder.getUserService()::getUser)
                    .flatMap(p -> {
                        var retrieveSelf = retrieveSelfUserForDMs
                                ? client.withRetrievalStrategy(EntityRetrievalStrategy.STORE)
                                .getUserById(client.getSelfId())
                                : Mono.<User>empty();
                        return retrieveSelf
                                .mapNotNull(u -> EntityFactory.createChat(client, p, u))
                                .switchIfEmpty(Mono.fromSupplier(() -> EntityFactory.createChat(client, p, null)));
                    });
        });
    }

    @Override
    public Mono<Chat> getChatFullById(Id chatId) {
        return Mono.defer(() -> switch (chatId.getType()) {
            case CHAT -> serviceHolder.getChatService().getFullChat(chatId.asLong())
                    .mapNotNull(c -> EntityFactory.createChat(client, c, null));
            case CHANNEL -> client.asInputChannel(chatId)
                    .flatMap(serviceHolder.getChatService()::getFullChannel)
                    .mapNotNull(c -> EntityFactory.createChat(client, c, null));
            case USER -> client.asInputUser(chatId)
                    .flatMap(serviceHolder.getUserService()::getFullUser)
                    .flatMap(p -> {
                        var retrieveSelf = retrieveSelfUserForDMs
                                ? client.withRetrievalStrategy(EntityRetrievalStrategy.STORE)
                                .getUserById(client.getSelfId())
                                : Mono.<User>empty();
                        return retrieveSelf
                                .mapNotNull(u -> EntityFactory.createChat(client, p, u))
                                .switchIfEmpty(Mono.fromSupplier(() -> EntityFactory.createChat(client, p, null)));
                    });
        });
    }

    @Override
    public Mono<ChatParticipant> getParticipantById(Id chatId, Id peerId) {
        if (chatId.getType() == Id.Type.CHAT && peerId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: USER, " +
                    "but given: " + chatId.getType()));
        }

        return Mono.defer(() -> switch (chatId.getType()) {
            // telegram api doest provide normal way to retrieve specified chat participant,
            // and therefore we retrieve full chat info and looking up participant
            case CHAT -> serviceHolder.getChatService().getFullChat(chatId.asLong())
                    .mapNotNull(c -> {
                        var chatFull = (BaseChatFull) c.fullChat();
                        telegram4j.tl.ChatParticipant member;
                        if (chatFull.participants() instanceof BaseChatParticipants base) {
                            member = base.participants().stream()
                                    .filter(p -> p.userId() == peerId.asLong())
                                    .findFirst()
                                    .orElse(null);

                        } else if (chatFull.participants() instanceof ChatParticipantsForbidden forbidden) {
                            member = Optional.ofNullable(forbidden.selfParticipant())
                                    .filter(p -> p.userId() == peerId.asLong())
                                    .orElse(null);
                        } else {
                            throw new IllegalStateException();
                        }

                        if (member == null) {
                            return null;
                        }
                        var user = c.users().stream()
                                .filter(u -> u.id() == peerId.asLong())
                                .findFirst()
                                .map(u -> EntityFactory.createUser(client, u))
                                .orElse(null);
                        return new ChatParticipant(client, user, member, chatId);
                    });
            case CHANNEL -> client.asInputChannel(chatId)
                    .switchIfEmpty(MappingUtil.unresolvedPeer(chatId))
                    .zipWith(client.asInputPeer(peerId)
                            .switchIfEmpty(MappingUtil.unresolvedPeer(peerId)))
                    .flatMap(TupleUtils.function(serviceHolder.getChatService()::getParticipant))
                    .map(p -> EntityFactory.createChannelParticipant(client, p, chatId, peerId));
            default -> Mono.error(new IllegalArgumentException("Incorrect id type, expected: CHANNEL or " +
                    "CHAT, but given: " + chatId.getType()));
        });
    }

    @Override
    public Flux<ChatParticipant> getParticipants(Id chatId) {
        return Flux.defer(() -> switch (chatId.getType()) {
            case CHAT -> serviceHolder.getChatService().getFullChat(chatId.asLong())
                    .flatMapMany(c -> {
                        var chatFull = (BaseChatFull) c.fullChat();
                        if (!(chatFull.participants() instanceof BaseChatParticipants base)) {
                            return Flux.empty();
                        }
                        var participants = base.participants();

                        var users = c.users().stream()
                                .map(u -> EntityFactory.createUser(client, u))
                                .filter(Objects::nonNull)
                                .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

                        return Flux.fromIterable(participants)
                                .map(p -> new ChatParticipant(client, users.get(Id.ofUser(p.userId())), p, chatId));
                    });
            case CHANNEL -> client.asInputChannel(chatId)
                    .switchIfEmpty(MappingUtil.unresolvedPeer(chatId))
                    .flatMapMany(channel -> {
                        Id channelId = Id.of(channel, client.getSelfId());
                        return PaginationSupport.paginate(o -> client.getServiceHolder().getChatService()
                                                .getParticipants(channel, ImmutableChannelParticipantsSearch.of(""), o, 200, 0),
                                        BaseChannelParticipants::count, 0, 200)
                                .flatMap(data -> {
                                    var chats = data.chats().stream()
                                            .map(c -> (Channel) EntityFactory.createChat(client, c, null))
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));
                                    var users = data.users().stream()
                                            .map(u -> EntityFactory.createUser(client, u))
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toMap(PeerEntity::getId, Function.identity()));

                                    return Flux.fromIterable(data.participants())
                                            .map(c -> {
                                                Id peerId = Id.of(TlEntityUtil.getUserId(c));
                                                MentionablePeer peer = switch (peerId.getType()) {
                                                    case USER -> users.get(peerId);
                                                    case CHANNEL -> chats.get(peerId);
                                                    default -> throw new IllegalStateException();
                                                };

                                                return new ChatParticipant(client, peer, c, channelId);
                                            });
                                });
                    });
            default -> Mono.error(new IllegalArgumentException("Incorrect id type, expected: CHANNEL or " +
                    "CHAT, but given: " + chatId.getType()));
        });
    }

    @Override
    public Mono<AuxiliaryMessages> getMessages(@Nullable Id chatId, Iterable<? extends InputMessage> messageIds) {
        return Mono.defer(() -> {
                    if (chatId == null || chatId.getType() != Id.Type.CHANNEL) {
                        return serviceHolder.getChatService().getMessages(messageIds);
                    }
                    return client.asInputChannel(chatId)
                            .switchIfEmpty(MappingUtil.unresolvedPeer(chatId))
                            .flatMap(c -> serviceHolder.getChatService().getMessages(c, messageIds));
                })
                .filter(m -> m.identifier() != MessagesNotModified.ID) // just ignore
                .map(d -> AuxiliaryEntityFactory.createMessages(client, d));
    }
}
