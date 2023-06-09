package telegram4j.mtproto.store;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.NetUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.*;
import telegram4j.mtproto.auth.AuthKey;
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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static telegram4j.mtproto.util.CryptoUtil.toByteBuf;

public class FileStoreLayout implements StoreLayout {

    protected static final Logger log = Loggers.getLogger(FileStoreLayout.class);

    protected static final Path DEFAULT_DATA_FILE = Path.of("./t4j.bin");

    protected final StoreLayout entityDelegate;
    protected final Path dataFile;
    protected final ConcurrentHashMap<Integer, AuthKey> authKeys = new ConcurrentHashMap<>();
    protected final AtomicBoolean saving = new AtomicBoolean();
    protected final ExecutorService persistExecutor;

    protected volatile int mainDcId;
    protected volatile long selfId;
    // TODO remove default values
    protected volatile DcOptions dcOptions = DcOptions.createDefault(false);
    protected volatile PublicRsaKeyRegister publicRsaKeyRegister = PublicRsaKeyRegister.createDefault();
    protected volatile State state;

    public FileStoreLayout(StoreLayout entityDelegate) {
        this(entityDelegate, DEFAULT_DATA_FILE, Executors.newSingleThreadExecutor());
    }

    public FileStoreLayout(StoreLayout entityDelegate, Path dataFile) {
        this(entityDelegate, dataFile, Executors.newSingleThreadExecutor());
    }

    public FileStoreLayout(StoreLayout entityDelegate, Path dataFile, ExecutorService persistExecutor) {
        this.dataFile = Objects.requireNonNull(dataFile);
        this.entityDelegate = Objects.requireNonNull(entityDelegate);
        this.persistExecutor = persistExecutor;
    }

    public Path getDataFile() {
        return dataFile;
    }

    // region serialization

    protected static void serializeDc(ByteBuf buf, DataCenter dc) {
        buf.writeByte(dc.getType().ordinal());
        buf.writeShortLE(dc.getId());
        buf.writeShortLE(dc.getPort());
        buf.writeByte(computeFlags(dc));
        buf.writeBytes(NetUtil.createByteArrayFromIpAddressString(dc.getAddress()));
        dc.getSecret().ifPresent(secret -> TlSerialUtil.serializeBytes(buf, secret));
    }

    protected interface Deserializer {

        Settings deserialize(ByteBuf buf);
    }

    protected static class Rev0Deserializer implements Deserializer {

        @Override
        public Settings deserialize(ByteBuf buf) {
            int mainDcId = buf.readUnsignedShortLE();
            long selfId = buf.readLongLE();
            int authKeysCount = buf.readUnsignedShortLE();
            var authKeys = new HashMap<Integer, AuthKey>(authKeysCount);
            for (int i = 0; i < authKeysCount; i++) {
                int dcId = buf.readUnsignedShortLE();
                // unused authKeyId
                buf.readLongLE();

                ByteBuf authKey = TlSerialUtil.deserializeBytes(buf);
                authKeys.put(dcId, new AuthKey(authKey));
            }

            byte dcOptionsFlags = buf.readByte();
            int dcOptionsCount = buf.readUnsignedShortLE();
            var options = new ArrayList<DataCenter>(dcOptionsCount);
            for (int i = 0; i < dcOptionsCount; i++) {
                DataCenter.Type type = DataCenter.Type.values()[buf.readUnsignedByte()];
                int id = buf.readUnsignedShortLE();
                int port = buf.readUnsignedShortLE();
                byte flags = buf.readByte();
                byte[] addb = new byte[(flags & IPV6_MASK) != 0 ? 16 : 4];
                buf.readBytes(addb);
                String address = NetUtil.bytesToIpAddress(addb);
                ByteBuf secret = (flags & SECRET_MASK) != 0 ? TlSerialUtil.deserializeBytes(buf) : null;
                options.add(DataCenter.create(type, (flags & TEST_MASK) != 0, id,
                        address, port, (flags & TCPO_ONLY_MASK) != 0,
                        (flags & STATIC_MASK) != 0, (flags & THIS_PORT_ONLY_MASK) != 0, secret));
            }

            var dcOptions = DcOptions.create(options, (dcOptionsFlags & TEST_MASK) != 0,
                    (dcOptionsFlags & PREFER_IPV6_MASK) != 0);

            int publicRsaKeyCount = buf.readUnsignedShortLE();
            var keys = new ArrayList<PublicRsaKey>(publicRsaKeyCount);
            for (int i = 0; i < publicRsaKeyCount; i++) {
                BigInteger exponent = CryptoUtil.fromByteBuf(TlSerialUtil.deserializeBytes(buf).retain());
                BigInteger modulus  = CryptoUtil.fromByteBuf(TlSerialUtil.deserializeBytes(buf).retain());
                keys.add(PublicRsaKey.create(exponent, modulus));
            }
            var publicRsaKeyRegister = PublicRsaKeyRegister.create(keys);
            State state = buf.readByte() == 1 ? TlDeserializer.deserialize(buf) : null;
            return new Settings(mainDcId, selfId, authKeys, dcOptions, publicRsaKeyRegister, state);
        }
    }

    static class Rev1Deserializer implements Deserializer {

        @Override
        public Settings deserialize(ByteBuf buf) {
            int mainDcId = buf.readUnsignedShortLE();
            long selfId = buf.readLongLE();
            int authKeysCount = buf.readUnsignedShortLE();
            var authKeys = new HashMap<Integer, AuthKey>(authKeysCount);
            for (int i = 0; i < authKeysCount; i++) {
                int dcId = buf.readUnsignedShortLE();
                ByteBuf authKey = TlSerialUtil.deserializeBytes(buf);
                authKeys.put(dcId, new AuthKey(authKey));
            }

            byte dcOptionsFlags = buf.readByte();
            int dcOptionsCount = buf.readUnsignedShortLE();
            var options = new ArrayList<DataCenter>(dcOptionsCount);
            var values = DataCenter.Type.values();
            for (int i = 0; i < dcOptionsCount; i++) {
                DataCenter.Type type = values[buf.readUnsignedByte()];
                int id = buf.readUnsignedShortLE();
                int port = buf.readUnsignedShortLE();
                byte flags = buf.readByte();
                byte[] addb = new byte[(flags & IPV6_MASK) != 0 ? 16 : 4];
                buf.readBytes(addb);
                String address = NetUtil.bytesToIpAddress(addb);
                ByteBuf secret = (flags & SECRET_MASK) != 0 ? TlSerialUtil.deserializeBytes(buf) : null;
                options.add(DataCenter.create(type, (flags & TEST_MASK) != 0, id,
                        address, port, (flags & TCPO_ONLY_MASK) != 0,
                        (flags & STATIC_MASK) != 0, (flags & THIS_PORT_ONLY_MASK) != 0, secret));
            }

            var dcOptions = DcOptions.create(options, (dcOptionsFlags & TEST_MASK) != 0,
                    (dcOptionsFlags & PREFER_IPV6_MASK) != 0);

            int publicRsaKeyCount = buf.readUnsignedShortLE();
            var keys = new ArrayList<PublicRsaKey>(publicRsaKeyCount);
            for (int i = 0; i < publicRsaKeyCount; i++) {
                BigInteger exponent = CryptoUtil.fromByteBuf(TlSerialUtil.deserializeBytes(buf).retain());
                BigInteger modulus  = CryptoUtil.fromByteBuf(TlSerialUtil.deserializeBytes(buf).retain());
                keys.add(PublicRsaKey.create(exponent, modulus));
            }
            var publicRsaKeyRegister = PublicRsaKeyRegister.create(keys);
            State state = buf.readByte() == 1 ? TlDeserializer.deserialize(buf) : null;
            return new Settings(mainDcId, selfId, authKeys, dcOptions, publicRsaKeyRegister, state);
        }
    }

    protected enum Version {
        REVISION0(new Rev0Deserializer()),
        REVISION1(new Rev1Deserializer()),
        CURRENT(REVISION1);

        final Deserializer deser;
        final short revision;

        Version(Version other) {
            this.deser = other.deser;
            this.revision = other.revision;
        }

        Version(Deserializer deser) {
            this.deser = deser;
            this.revision = (short) ordinal();
        }

        protected static Version of(int s) {
            return switch (s) {
                case 0 ->  REVISION0;
                case 1 -> REVISION1;
                default -> throw new IllegalStateException("Unknown version id: " + s);
            };
        }
    }

    protected static final byte PREFER_IPV6_MASK = 0b10;

    protected static byte computeFlags(DcOptions dcOptions) {
        byte flags = 0;
        flags |= dcOptions.isTest() ? TEST_MASK : 0;
        flags |= dcOptions.isPreferIpv6() ? PREFER_IPV6_MASK : 0;
        return flags;
    }

    protected static final byte TEST_MASK           = 0b000001;
    protected static final byte STATIC_MASK         = 0b000010;
    protected static final byte TCPO_ONLY_MASK      = 0b000100;
    protected static final byte SECRET_MASK         = 0b001000;
    protected static final byte IPV6_MASK           = 0b010000;
    protected static final byte THIS_PORT_ONLY_MASK = 0b100000;

    protected static byte computeFlags(DataCenter dc) {
        byte flags = 0;
        flags |= dc.isTest() ? TEST_MASK : 0;
        flags |= dc.isStatic() ? STATIC_MASK : 0;
        flags |= dc.isTcpObfuscatedOnly() ? TCPO_ONLY_MASK : 0;
        flags |= dc.isThisPortOnly() ? THIS_PORT_ONLY_MASK : 0;
        flags |= dc.isIpv6() ? IPV6_MASK : 0;
        flags |= dc.getSecret().map(b -> SECRET_MASK).orElse((byte) 0);
        return flags;
    }

    protected record Settings(int mainDcId, long selfId, Map<Integer, AuthKey> authKeys, DcOptions dcOptions,
                              PublicRsaKeyRegister publicRsaKeyRegister, @Nullable State state) {

        protected Settings {
            requireSize(authKeys, "authKeys");
            requireSize(dcOptions.getBackingList(), "dcOptions");
            requireSize(publicRsaKeyRegister.getBackingMap(), "pubRsaKeyRegister");
        }

        void serialize(ByteBuf buf) {
            buf.writeShortLE(Version.CURRENT.revision);
            buf.writeShortLE(mainDcId);
            buf.writeLongLE(selfId);
            buf.writeShortLE(authKeys.size());
            authKeys.forEach((dcId, authKey) -> {
                buf.writeShortLE(dcId);
                TlSerialUtil.serializeBytes(buf, authKey.value());
            });
            buf.writeByte(computeFlags(dcOptions));
            buf.writeShortLE(dcOptions.getBackingList().size());
            for (DataCenter dc : dcOptions.getBackingList()) {
                serializeDc(buf, dc);
            }
            buf.writeShortLE(publicRsaKeyRegister.getBackingMap().size());
            for (PublicRsaKey key : publicRsaKeyRegister.getBackingMap().values()) {
                TlSerialUtil.serializeBytes(buf, toByteBuf(key.getExponent()));
                TlSerialUtil.serializeBytes(buf, toByteBuf(key.getModulus()));
            }
            if (state != null) {
                buf.writeByte(1);
                TlSerializer.serialize(buf, state);
            } else {
                buf.writeByte(0);
            }
        }
    }

    protected static final int MAX_COLLECTIONS_SIZE = 0xffff;

    protected static void requireSize(Map<?, ?> c, String msg) {
        if (c.size() > MAX_COLLECTIONS_SIZE) {
            throw new IllegalStateException("Too big " + msg + " collection");
        }
    }

    protected static void requireSize(Collection<?> c, String msg) {
        if (c.size() > MAX_COLLECTIONS_SIZE) {
            throw new IllegalStateException("Too big " + msg + " collection");
        }
    }

    // endregion

    protected Settings copySettings() {
        return new Settings(mainDcId, selfId, authKeys, dcOptions, publicRsaKeyRegister, state);
    }

    protected boolean isAssociatedToUser() {
        return selfId != 0;
    }

    protected Mono<Void> trySave() {
        return Mono.create(sink -> {
            if (!isAssociatedToUser()) {
                sink.success();
                return;
            }

            if (!saving.get() && saving.compareAndSet(false, true)) {
                Settings settings = copySettings();

                persistExecutor.execute(() -> {
                    if (log.isDebugEnabled()) {
                        log.debug("Saving information for main DC {} to {}", settings.mainDcId, dataFile);
                    }

                    try {
                        ByteBuf data = Unpooled.buffer();
                        settings.serialize(data);
                        Files.write(dataFile, CryptoUtil.toByteArray(data), StandardOpenOption.WRITE, StandardOpenOption.CREATE);
                    } catch (IOException e) {
                        sink.error(e);
                        saving.set(false);
                        return;
                    }

                    saving.set(false);
                    sink.success();
                });
            } else {
                sink.success();
            }
        });
    }

    @Override
    public Mono<Void> initialize() {
        return Mono.fromRunnable(() -> {
            if (Files.notExists(dataFile)) {
                return;
            }

            try {
                ByteBuf buf = Unpooled.wrappedBuffer(Files.readAllBytes(dataFile));
                Version ver = Version.of(buf.readUnsignedShortLE());
                Settings sett = ver.deser.deserialize(buf);
                mainDcId = sett.mainDcId;
                selfId = sett.selfId;
                authKeys.putAll(sett.authKeys);
                dcOptions = sett.dcOptions;
                publicRsaKeyRegister = sett.publicRsaKeyRegister;
                state = sett.state;

                if (log.isDebugEnabled()) {
                    log.debug("Loaded information for main DC {} from {}", sett.mainDcId, dataFile);
                }
            } catch (IOException e) {
                log.warn("Failed to load information from " + dataFile, e);
                throw new UncheckedIOException(e);
            }
        });
    }

    @Override
    public Mono<Void> close() {
        return entityDelegate.close()
                .and(trySave()
                        .then(Mono.fromRunnable(persistExecutor::shutdown)));
    }

    @Override
    public Mono<DataCenter> getDataCenter() {
        return Mono.fromSupplier(() -> mainDcId)
                .filter(id -> id != 0)
                .map(id -> dcOptions.find(DcId.Type.MAIN, id).orElseThrow());
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
    public Mono<AuthKey> getAuthKey(DataCenter dc) {
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
                .and(Mono.defer(() -> {
                    if (mainDcId == dc.getId()) {
                        return Mono.empty();
                    }

                    this.mainDcId = dc.getId();
                    return trySave();
                }));
    }

    @Override
    public Mono<Void> updateState(State state) {
        return entityDelegate.updateState(state)
                .and(Mono.defer(() -> {
                    if (state.equals(this.state)) {
                        return Mono.empty();
                    }
                    this.state = state;
                    return trySave();
                }));
    }

    @Override
    public Mono<Void> updateDcOptions(DcOptions dcOptions) {
        return entityDelegate.updateDcOptions(dcOptions)
                .and(Mono.defer(() -> {
                    if (dcOptions.equals(this.dcOptions)) {
                        return Mono.empty();
                    }
                    this.dcOptions = dcOptions;
                    return trySave();
                }));
    }

    @Override
    public Mono<Void> updatePublicRsaKeyRegister(PublicRsaKeyRegister publicRsaKeyRegister) {
        return entityDelegate.updatePublicRsaKeyRegister(publicRsaKeyRegister)
                .and(Mono.defer(() -> {
                    if (publicRsaKeyRegister.equals(this.publicRsaKeyRegister)) {
                        return Mono.empty();
                    }
                    this.publicRsaKeyRegister = publicRsaKeyRegister;
                    return trySave();
                }));
    }

    @Override
    public Mono<Void> onAuthorization(BaseAuthorization auth) {
        return entityDelegate.onAuthorization(auth)
                .and(Mono.defer(() -> {
                    this.selfId = auth.user().id();
                    return trySave();
                }));
    }

    @Override
    public Mono<Void> updateAuthKey(DataCenter dc, AuthKey authKey) {
        return entityDelegate.updateAuthKey(dc, authKey)
                .and(Mono.defer(() -> {
                    authKeys.put(dc.getId(), authKey);
                    return trySave();
                }));
    }

    // delegation

    @Override
    public Mono<Void> onUpdateConfig(Config config) {
        return entityDelegate.onUpdateConfig(config)
                .and(Mono.defer(() -> {
                    this.dcOptions = DcOptions.from(config);
                    return trySave();
                }));
    }

    @Override
    public Mono<Config> getConfig() {
        return entityDelegate.getConfig();
    }

    //////

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
    public Mono<Boolean> existMessage(Peer peerId, int messageId) {
        return entityDelegate.existMessage(peerId, messageId);
    }

    @Override
    public Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteMessages update) {
        return entityDelegate.onDeleteMessages(update);
    }

    @Override
    public Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteScheduledMessages update) {
        return entityDelegate.onDeleteMessages(update);
    }

    @Override
    public Mono<ResolvedDeletedMessages> onDeleteMessages(UpdateDeleteChannelMessages update) {
        return entityDelegate.onDeleteMessages(update);
    }

    @Override
    public Mono<Void> onUpdatePinnedMessages(UpdatePinnedMessages payload) {
        return entityDelegate.onUpdatePinnedMessages(payload);
    }

    @Override
    public Mono<Void> onUpdatePinnedMessages(UpdatePinnedChannelMessages payload) {
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
