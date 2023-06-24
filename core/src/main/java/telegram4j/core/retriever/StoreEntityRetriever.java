package telegram4j.core.retriever;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.auxiliary.AuxiliaryMessages;
import telegram4j.core.internal.AuxiliaryEntityFactory;
import telegram4j.core.internal.EntityFactory;
import telegram4j.core.object.PeerEntity;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.ChatParticipant;
import telegram4j.core.util.Id;
import telegram4j.core.util.PeerId;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.InputMessage;
import telegram4j.tl.PeerChannel;
import telegram4j.tl.PeerUser;
import telegram4j.tl.messages.MessagesNotModified;

/** Implementation of {@code EntityRetriever} which uses {@link StoreLayout storage}. */
public class StoreEntityRetriever implements EntityRetriever {

    private final MTProtoTelegramClient client;
    private final StoreLayout storeLayout;
    private final boolean retrieveSelfUserForDMs;

    public StoreEntityRetriever(MTProtoTelegramClient client) {
        this(client, true);
    }

    public StoreEntityRetriever(MTProtoTelegramClient client, boolean retrieveSelfUserForDMs) {
        this(client, client.getMtProtoResources().getStoreLayout(), retrieveSelfUserForDMs);
    }

    private StoreEntityRetriever(MTProtoTelegramClient client, StoreLayout storeLayout, boolean retrieveSelfUserForDMs) {
        this.client = client;
        this.storeLayout = storeLayout;
        this.retrieveSelfUserForDMs = retrieveSelfUserForDMs;
    }

    public StoreEntityRetriever withRetrieveSelfUserForDMs(boolean state) {
        if (retrieveSelfUserForDMs == state) return this;
        return new StoreEntityRetriever(client, storeLayout, state);
    }

    @Override
    public Mono<PeerEntity> resolvePeer(PeerId peerId) {
        Mono<PeerEntity> resolveById = Mono.justOrEmpty(peerId.asId())
                .flatMap(id -> switch (id.getType()) {
                    case CHAT, CHANNEL -> getChatMinById(id);
                    case USER -> getUserMinById(id);
                });

        return Mono.justOrEmpty(peerId.asUsername())
                .flatMap(storeLayout::resolvePeer)
                .flatMap(p -> switch (p.peer().identifier()) {
                    case PeerChannel.ID ->
                            Mono.justOrEmpty(EntityFactory.createChat(client, p.chats().get(0), null));
                    case PeerUser.ID -> Mono.justOrEmpty(EntityFactory.createUser(client, p.users().get(0)));
                    default -> Mono.error(new IllegalStateException("Unknown Peer type: " + p.peer()));
                })
                .switchIfEmpty(resolveById);
    }

    @Override
    public Mono<User> getUserById(Id userId) {
        if (userId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.USER + ", but given: " + userId.getType()));
        }

        return storeLayout.getUserById(userId.asLong())
                .map(u -> new User(client, u.minData, u.fullData));
    }

    @Override
    public Mono<User> getUserMinById(Id userId) {
        if (userId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.USER + ", but given: " + userId.getType()));
        }

        return storeLayout.getUserMinById(userId.asLong())
                .map(u -> new User(client, u, null));
    }

    @Override
    public Mono<User> getUserFullById(Id userId) {
        if (userId.getType() != Id.Type.USER) {
            return Mono.error(new IllegalArgumentException("Incorrect id type, expected: "
                    + Id.Type.USER + ", but given: " + userId.getType()));
        }

        return storeLayout.getUserFullById(userId.asLong())
                .mapNotNull(u -> EntityFactory.createUser(client, u));
    }

    @Override
    public Mono<Chat> getChatById(Id chatId) {
        return Mono.defer(() -> switch (chatId.getType()) {
            case CHAT -> storeLayout.getChatById(chatId.asLong())
                    .mapNotNull(c -> EntityFactory.createChat(client, c, null));
            case CHANNEL -> storeLayout.getChannelById(chatId.asLong())
                    .mapNotNull(c -> EntityFactory.createChat(client, c, null));
            case USER -> storeLayout.getUserById(chatId.asLong())
                    .flatMap(p -> {
                        var retrieveSelf = retrieveSelfUserForDMs
                                ? getUserById(client.getSelfId())
                                : Mono.<User>empty();
                        return retrieveSelf
                                .map(u -> EntityFactory.createChat(client, p, u))
                                .switchIfEmpty(Mono.fromSupplier(() -> EntityFactory.createChat(client, p, null)));
                    });
        });
    }

    @Override
    public Mono<Chat> getChatMinById(Id chatId) {
        return Mono.defer(() -> switch (chatId.getType()) {
            case CHAT -> storeLayout.getChatMinById(chatId.asLong())
                    .mapNotNull(c -> EntityFactory.createChat(client, c, null));
            case CHANNEL -> storeLayout.getChannelMinById(chatId.asLong())
                    .mapNotNull(c -> EntityFactory.createChat(client, c, null));
            case USER -> storeLayout.getUserMinById(chatId.asLong())
                    .flatMap(p -> {
                        var retrieveSelf = retrieveSelfUserForDMs
                                ? getUserById(client.getSelfId())
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
            case CHAT -> storeLayout.getChatFullById(chatId.asLong())
                    .mapNotNull(c -> EntityFactory.createChat(client, c, null));
            case CHANNEL -> storeLayout.getChannelFullById(chatId.asLong())
                    .mapNotNull(c -> EntityFactory.createChat(client, c, null));
            case USER -> storeLayout.getUserFullById(chatId.asLong())
                    .flatMap(p -> {
                        var retrieveSelf = retrieveSelfUserForDMs
                                ? getUserById(client.getSelfId())
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
            case CHAT -> storeLayout.getChatParticipantById(chatId.asLong(), peerId.asLong())
                    .map(r -> new ChatParticipant(client, r.getUser()
                            .map(u -> EntityFactory.createUser(client, u))
                            .orElse(null), r.getParticipant(), chatId));
            case CHANNEL -> storeLayout.getChannelParticipantById(chatId.asLong(), peerId.asPeer())
                    .map(p -> EntityFactory.createChannelParticipant(client, p, chatId, peerId));
            default -> Mono.error(new IllegalArgumentException("Incorrect id type, expected: CHANNEL or " +
                    "CHAT, but given: " + chatId.getType()));
        });
    }

    @Override
    public Flux<ChatParticipant> getParticipants(Id chatId) {
        return Flux.defer(() -> switch (chatId.getType()) {
            case CHAT -> storeLayout.getChatParticipants(chatId.asLong())
                    .map(r -> new ChatParticipant(client, r.getUser()
                            .map(u -> EntityFactory.createUser(client, u))
                            .orElse(null), r.getParticipant(), chatId));
            case CHANNEL -> storeLayout.getChannelParticipants(chatId.asLong())
                    .map(p -> EntityFactory.createChannelParticipant(client, p, chatId,
                            Id.of(TlEntityUtil.getUserId(p.participant()))));
            default -> Mono.error(new IllegalArgumentException("Incorrect id type, expected: CHANNEL or " +
                    "CHAT, but given: " + chatId.getType()));
        });
    }

    @Override
    public Mono<AuxiliaryMessages> getMessages(@Nullable Id chatId, Iterable<? extends InputMessage> messageIds) {
        return Mono.defer(() -> {
            if (chatId == null || chatId.getType() != Id.Type.CHANNEL) {
                return storeLayout.getMessages(messageIds);
            }
            return storeLayout.getMessages(chatId.asLong(), messageIds);
        })
        .filter(m -> m.identifier() != MessagesNotModified.ID)
        .map(d -> AuxiliaryEntityFactory.createMessages(client, d));
    }
}
