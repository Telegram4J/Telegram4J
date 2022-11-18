package telegram4j.mtproto.store;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.*;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.mtproto.store.object.*;
import telegram4j.mtproto.util.CryptoUtil;
import telegram4j.tl.*;
import telegram4j.tl.auth.BaseAuthorization;
import telegram4j.tl.channels.BaseChannelParticipants;
import telegram4j.tl.channels.ChannelParticipant;
import telegram4j.tl.contacts.ResolvedPeer;
import telegram4j.tl.messages.ChatFull;
import telegram4j.tl.messages.Messages;
import telegram4j.tl.updates.State;
import telegram4j.tl.users.UserFull;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static telegram4j.mtproto.util.CryptoUtil.toByteBuf;

public class FileStoreLayout implements StoreLayout {

    private static final Logger log = Loggers.getLogger(FileStoreLayout.class);

    private static final Path DEFAULT_DATA_FILE = Path.of("./t4j.bin");

    private static final VarHandle SAVING;

    static {
        try {
            var l = MethodHandles.lookup();
            SAVING = l.findVarHandle(FileStoreLayout.class,  "saving", boolean.class);
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    final StoreLayout entityDelegate;
    final Path dataFile;
    final ConcurrentHashMap<Integer, AuthorizationKeyHolder> authKeys = new ConcurrentHashMap<>();

    volatile int mainDcId;
    volatile long selfId;
    volatile DcOptions dcOptions;
    volatile PublicRsaKeyRegister publicRsaKeyRegister;
    volatile State state;
    volatile boolean saving;

    public FileStoreLayout(StoreLayout entityDelegate) {
        this(entityDelegate, DEFAULT_DATA_FILE);
    }

    public FileStoreLayout(StoreLayout entityDelegate, Path dataFile) {
        this.dataFile = Objects.requireNonNull(dataFile);
        this.entityDelegate = Objects.requireNonNull(entityDelegate);
    }

    public Path getDataFile() {
        return dataFile;
    }

    static class Settings {
        final int mainDcId;
        final long selfId;
        final Map<Integer, AuthorizationKeyHolder> authKeys;
        final DcOptions dcOptions;
        final PublicRsaKeyRegister publicRsaKeyRegister;
        @Nullable
        final State state;

        Settings(int mainDcId, long selfId, Map<Integer, AuthorizationKeyHolder> authKeys,
                 DcOptions dcOptions, PublicRsaKeyRegister publicRsaKeyRegister, @Nullable State state) {
            this.mainDcId = mainDcId;
            this.selfId = selfId;
            this.authKeys = authKeys;
            this.dcOptions = dcOptions;
            this.publicRsaKeyRegister = publicRsaKeyRegister;
            this.state = state;
        }

        void serialize(ByteBuf buf) {
            buf.writeIntLE(mainDcId);
            buf.writeLongLE(selfId);
            buf.writeIntLE(authKeys.size());
            authKeys.forEach((dcId, authKey) -> {
                buf.writeIntLE(dcId);
                buf.writeLongLE(authKey.getAuthKeyId());
                TlSerialUtil.serializeBytes(buf, authKey.getAuthKey());
            });
            buf.writeByte(computeFlags(dcOptions));
            buf.writeIntLE(dcOptions.getBackingList().size());
            for (DataCenter dc : dcOptions.getBackingList()) {
                buf.writeByte(dc.getType().ordinal());
                buf.writeIntLE(dc.getId());
                buf.writeIntLE(dc.getPort());
                buf.writeByte(computeFlags(dc));
                TlSerialUtil.serializeString(buf, dc.getAddress());
                dc.getSecret().ifPresent(secret -> TlSerialUtil.serializeBytes(buf, secret));
            }
            buf.writeIntLE(publicRsaKeyRegister.getBackingMap().size());
            for (PublicRsaKey key : publicRsaKeyRegister.getBackingMap().values()) {
                TlSerialUtil.serializeBytes(buf, toByteBuf(key.getExponent()));
                TlSerialUtil.serializeBytes(buf, toByteBuf(key.getModulus()));
            }
            buf.writeByte(state != null ? 1 : 0);
            if (state != null) {
                TlSerializer.serialize(buf, state);
            }
        }
    }

    static Settings deserializeSettings(ByteBuf buf) {
        int mainDcId = buf.readIntLE();
        long selfId = buf.readLongLE();
        int authKeysCount = buf.readIntLE();
        Map<Integer, AuthorizationKeyHolder> authKeys = new HashMap<>(authKeysCount);
        for (int i = 0; i < authKeysCount; i++) {
            int dcId = buf.readIntLE();
            long authKeyId = buf.readLongLE();
            ByteBuf authKey = TlSerialUtil.deserializeBytes(buf);
            authKeys.put(dcId, new AuthorizationKeyHolder(authKey, authKeyId));
        }

        byte dcOptionsFlags = buf.readByte();
        int dcOptionsCount = buf.readIntLE();
        List<DataCenter> options = new ArrayList<>(dcOptionsCount);
        for (int i = 0; i < dcOptionsCount; i++) {
            DataCenter.Type type = DataCenter.Type.values()[buf.readByte()];
            int id = buf.readIntLE();
            int port = buf.readIntLE();
            byte flags = buf.readByte();
            String address = TlSerialUtil.deserializeString(buf);
            ByteBuf secret = (flags & SECRET_MASK) != 0 ? TlSerialUtil.deserializeBytes(buf) : null;
            options.add(DataCenter.create(type, (flags & TEST_MASK) != 0, id,
                    address, port, (flags & TCPO_ONLY_MASK) != 0,
                    (flags & STATIC_MASK) != 0, secret));
        }

        var dcOptions = DcOptions.create(options, (dcOptionsFlags & TEST_MASK) != 0,
                (dcOptionsFlags & PREFER_IPV6_MASK) != 0);

        int publicRsaKeyCount = buf.readIntLE();
        List<PublicRsaKey> keys = new ArrayList<>(publicRsaKeyCount);
        for (int i = 0; i < publicRsaKeyCount; i++) {
            BigInteger exponent = CryptoUtil.fromByteBuf(TlSerialUtil.deserializeBytes(buf).retain());
            BigInteger modulus  = CryptoUtil.fromByteBuf(TlSerialUtil.deserializeBytes(buf).retain());
            keys.add(PublicRsaKey.create(exponent, modulus));
        }
        var publicRsaKeyRegister = PublicRsaKeyRegister.create(keys);
        State state = buf.readByte() == 1 ? TlDeserializer.deserialize(buf) : null;
        return new Settings(mainDcId, selfId, authKeys, dcOptions, publicRsaKeyRegister, state);
    }

    static final byte PREFER_IPV6_MASK = 0b10;

    static byte computeFlags(DcOptions dcOptions) {
        byte flags = 0;
        flags |= dcOptions.isTest() ? TEST_MASK : 0;
        flags |= dcOptions.isPreferIpv6() ? PREFER_IPV6_MASK : 0;
        return flags;
    }

    static final byte TEST_MASK      = 0b0001;
    static final byte STATIC_MASK    = 0b0010;
    static final byte TCPO_ONLY_MASK = 0b0100;
    static final byte SECRET_MASK    = 0b1000;

    static byte computeFlags(DataCenter dc) {
        byte flags = 0;
        flags |= dc.isTest() ? TEST_MASK : 0;
        flags |= dc.isStatic() ? STATIC_MASK : 0;
        flags |= dc.isTcpObfuscatedOnly() ? TCPO_ONLY_MASK : 0;
        flags |= dc.getSecret().map(b -> SECRET_MASK).orElse((byte) 0);
        return flags;
    }

    private Settings copySettings() {
        return new Settings(mainDcId, selfId, authKeys, dcOptions, publicRsaKeyRegister, state);
    }

    private boolean isAssociatedToUser() {
        return selfId != 0;
    }

    @Override
    public Mono<Void> initialize() {
        return Mono.fromCallable(() -> {
            if (Files.notExists(dataFile)) {
                return null;
            }

            ByteBuf buf = Unpooled.wrappedBuffer(Files.readAllBytes(dataFile));
            var sett = deserializeSettings(buf);
            mainDcId = sett.mainDcId;
            selfId = sett.selfId;
            authKeys.putAll(sett.authKeys);
            dcOptions = sett.dcOptions;
            publicRsaKeyRegister = sett.publicRsaKeyRegister;
            state = sett.state;
            log.debug("Loaded information for DC {}", sett.mainDcId);
            return null;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }

    private Mono<Void> save() {
        return Mono.fromCallable(() -> {
            if (!isAssociatedToUser()) {
                return null;
            }

            if (SAVING.compareAndSet(this, false, true)) {
                var settings = copySettings();

                log.debug("Saving information for DC {}", settings.mainDcId);
                ByteBuf data = Unpooled.buffer();
                settings.serialize(data);
                Files.write(dataFile, CryptoUtil.toByteArray(data), StandardOpenOption.WRITE, StandardOpenOption.CREATE);

                SAVING.setVolatile(this, false);
            }
            return null;
        });
    }

    @Override
    public Mono<DataCenter> getDataCenter() {
        return Mono.fromSupplier(() -> mainDcId)
                .filter(id -> id != 0)
                .map(id -> dcOptions.find(DcId.main(id)).orElseThrow());
    }

    @Override
    public Mono<State> getCurrentState() {
        return Mono.fromSupplier(() -> state);
    }

    @Override
    public Mono<DcOptions> getDcOptions() {
        return Mono.fromSupplier(() -> dcOptions);
    }

    @Override
    public Mono<PublicRsaKeyRegister> getPublicRsaKeyRegister() {
        return Mono.fromSupplier(() -> publicRsaKeyRegister);
    }

    @Override
    public Mono<AuthorizationKeyHolder> getAuthorizationKey(DataCenter dc) {
        return Mono.fromSupplier(() -> authKeys.get(dc.getId()));
    }

    @Override
    public Mono<Long> getSelfId() {
        return Mono.fromSupplier(() -> selfId)
                .filter(l -> l != 0);
    }

    @Override
    public Mono<Void> updateDataCenter(DataCenter dc) {
        return entityDelegate.updateDataCenter(dc)
                .publishOn(Schedulers.boundedElastic())
                .and(Mono.defer(() -> {
                    this.mainDcId = dc.getId();
                    return save();
                }));
    }

    @Override
    public Mono<Void> updateState(State state) {
        return entityDelegate.updateState(state)
                .publishOn(Schedulers.boundedElastic())
                .and(Mono.defer(() -> {
                    this.state = state;
                    return save();
                }));
    }

    @Override
    public Mono<Void> updateDcOptions(DcOptions dcOptions) {
        return entityDelegate.updateDcOptions(dcOptions)
                .publishOn(Schedulers.boundedElastic())
                .and(Mono.defer(() -> {
                    this.dcOptions = dcOptions;
                    return save();
                }));
    }

    @Override
    public Mono<Void> updatePublicRsaKeyRegister(PublicRsaKeyRegister publicRsaKeyRegister) {
        return entityDelegate.updatePublicRsaKeyRegister(publicRsaKeyRegister)
                .publishOn(Schedulers.boundedElastic())
                .and(Mono.defer(() -> {
                    this.publicRsaKeyRegister = publicRsaKeyRegister;
                    return save();
                }));
    }

    @Override
    public Mono<Void> onAuthorization(BaseAuthorization auth) {
        return entityDelegate.onAuthorization(auth)
                .publishOn(Schedulers.boundedElastic())
                .and(Mono.defer(() -> {
                    this.selfId = auth.user().id();
                    return save();
                }));
    }

    @Override
    public Mono<Void> updateAuthorizationKey(DataCenter dc, AuthorizationKeyHolder authKey) {
        return entityDelegate.updateAuthorizationKey(dc, authKey)
                .publishOn(Schedulers.boundedElastic())
                .and(Mono.defer(() -> {
                    authKeys.put(dc.getId(), authKey);
                    return save();
                }));
    }

    // delegation

    @Override
    public Mono<ChatData<BaseChat, BaseChatFull>> getChatById(long chatId) {
        return entityDelegate.getChatById(chatId);
    }

    @Override
    public Mono<ChatData<Channel, ChannelFull>> getChannelById(long channelId) {
        return entityDelegate.getChannelById(channelId);
    }

    @Override
    public Mono<PeerData<BaseUser, telegram4j.tl.UserFull>> getUserById(long userId) {
        return entityDelegate.getUserById(userId);
    }

    @Override
    public Mono<ResolvedPeer> resolvePeer(String username) {
        return entityDelegate.resolvePeer(username);
    }

    @Override
    public Mono<InputPeer> resolvePeer(Peer peerId) {
        return entityDelegate.resolvePeer(peerId);
    }

    @Override
    public Mono<InputUser> resolveUser(long userId) {
        return entityDelegate.resolveUser(userId);
    }

    @Override
    public Mono<InputChannel> resolveChannel(long channelId) {
        return entityDelegate.resolveChannel(channelId);
    }

    @Override
    public Mono<Boolean> existMessage(BaseMessageFields message) {
        return entityDelegate.existMessage(message);
    }

    @Override
    public Mono<Messages> getMessages(Iterable<? extends InputMessage> messageIds) {
        return entityDelegate.getMessages(messageIds);
    }

    @Override
    public Mono<Messages> getMessages(long channelId, Iterable<? extends InputMessage> messageIds) {
        return entityDelegate.getMessages(channelId, messageIds);
    }

    @Override
    public Mono<BaseChat> getChatMinById(long chatId) {
        return entityDelegate.getChatMinById(chatId);
    }

    @Override
    public Mono<ChatFull> getChatFullById(long chatId) {
        return entityDelegate.getChatFullById(chatId);
    }

    @Override
    public Mono<Channel> getChannelMinById(long channelId) {
        return entityDelegate.getChannelMinById(channelId);
    }

    @Override
    public Mono<ChatFull> getChannelFullById(long channelId) {
        return entityDelegate.getChannelFullById(channelId);
    }

    @Override
    public Mono<BaseUser> getUserMinById(long userId) {
        return entityDelegate.getUserMinById(userId);
    }

    @Override
    public Mono<UserFull> getUserFullById(long userId) {
        return entityDelegate.getUserFullById(userId);
    }

    @Override
    public Mono<ChannelParticipant> getChannelParticipantById(long channelId, Peer peerId) {
        return entityDelegate.getChannelParticipantById(channelId, peerId);
    }

    @Override
    public Flux<ChannelParticipant> getChannelParticipants(long channelId) {
        return entityDelegate.getChannelParticipants(channelId);
    }

    @Override
    public Mono<ResolvedChatParticipant> getChatParticipantById(long chatId, long userId) {
        return entityDelegate.getChatParticipantById(chatId, userId);
    }

    @Override
    public Flux<ResolvedChatParticipant> getChatParticipants(long chatId) {
        return entityDelegate.getChatParticipants(chatId);
    }

    @Override
    public Mono<MessagePoll> getPollById(long pollId) {
        return entityDelegate.getPollById(pollId);
    }

    @Override
    public Mono<Void> onNewMessage(Message update) {
        return entityDelegate.onNewMessage(update);
    }

    @Override
    public Mono<Message> onEditMessage(Message update) {
        return entityDelegate.onEditMessage(update);
    }

    @Override
    public Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteMessagesFields update) {
        return entityDelegate.onDeleteMessages(update);
    }

    @Override
    public Mono<Void> onUpdatePinnedMessages(UpdatePinnedMessagesFields payload) {
        return entityDelegate.onUpdatePinnedMessages(payload);
    }

    @Override
    public Mono<Void> onChatParticipant(UpdateChatParticipant payload) {
        return entityDelegate.onChatParticipant(payload);
    }

    @Override
    public Mono<Void> onChannelParticipant(UpdateChannelParticipant payload) {
        return entityDelegate.onChannelParticipant(payload);
    }

    @Override
    public Mono<Void> onChatParticipants(ChatParticipants payload) {
        return entityDelegate.onChatParticipants(payload);
    }

    @Override
    public Mono<Void> updateChannelPts(long channelId, int pts) {
        return entityDelegate.updateChannelPts(channelId, pts);
    }

    @Override
    public Mono<Void> registerPoll(Peer peerId, int messageId, InputMediaPoll poll) {
        return entityDelegate.registerPoll(peerId, messageId, poll);
    }

    @Override
    public Mono<Void> onContacts(Iterable<? extends Chat> chats, Iterable<? extends User> users) {
        return entityDelegate.onContacts(chats, users);
    }

    @Override
    public Mono<Void> onUserUpdate(UserFull payload) {
        return entityDelegate.onUserUpdate(payload);
    }

    @Override
    public Mono<Void> onChatUpdate(ChatFull payload) {
        return entityDelegate.onChatUpdate(payload);
    }

    @Override
    public Mono<Void> onChannelParticipants(long channelId, BaseChannelParticipants payload) {
        return entityDelegate.onChannelParticipants(channelId, payload);
    }

    @Override
    public Mono<Void> onChannelParticipant(long channelId, ChannelParticipant payload) {
        return entityDelegate.onChannelParticipant(channelId, payload);
    }

    @Override
    public Mono<Void> onMessages(Messages payload) {
        return entityDelegate.onMessages(payload);
    }
}
