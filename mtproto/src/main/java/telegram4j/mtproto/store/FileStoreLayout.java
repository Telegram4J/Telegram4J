package telegram4j.mtproto.store;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.NetUtil;
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

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static telegram4j.mtproto.util.CryptoUtil.toByteBuf;

public class FileStoreLayout implements StoreLayout {

    private static final Logger log = Loggers.getLogger(FileStoreLayout.class);

    private static final Path DEFAULT_DATA_FILE = Path.of("./t4j.bin");

    final StoreLayout entityDelegate;
    final Path dataFile;
    final ConcurrentHashMap<Integer, AuthorizationKeyHolder> authKeys = new ConcurrentHashMap<>();

    volatile int mainDcId;
    volatile long selfId;
    volatile DcOptions dcOptions;
    volatile PublicRsaKeyRegister publicRsaKeyRegister;
    volatile State state;
    final AtomicBoolean saving = new AtomicBoolean();

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

    // region serialization

    static void serializeDc(ByteBuf buf, DataCenter dc) {
        buf.writeByte(dc.getType().ordinal());
        buf.writeShortLE(dc.getId());
        buf.writeShortLE(dc.getPort());
        buf.writeByte(computeFlags(dc));
        buf.writeBytes(NetUtil.createByteArrayFromIpAddressString(dc.getAddress()));
        dc.getSecret().ifPresent(secret -> TlSerialUtil.serializeBytes(buf, secret));
    }

    interface Deserializer {

        Settings deserialize(ByteBuf buf);
    }

    static class Rev0Deserializer implements Deserializer {

        @Override
        public Settings deserialize(ByteBuf buf) {
            int mainDcId = buf.readUnsignedShortLE();
            long selfId = buf.readLongLE();
            int authKeysCount = buf.readUnsignedShortLE();
            Map<Integer, AuthorizationKeyHolder> authKeys = new HashMap<>(authKeysCount);
            for (int i = 0; i < authKeysCount; i++) {
                int dcId = buf.readUnsignedShortLE();
                long authKeyId = buf.readLongLE();
                ByteBuf authKey = TlSerialUtil.deserializeBytes(buf);
                authKeys.put(dcId, new AuthorizationKeyHolder(authKey, authKeyId));
            }

            byte dcOptionsFlags = buf.readByte();
            int dcOptionsCount = buf.readUnsignedShortLE();
            List<DataCenter> options = new ArrayList<>(dcOptionsCount);
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
    }

    enum Version {
        REVISION0(new Rev0Deserializer()),
        CURRENT(REVISION0);

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

        static Version of(int s) {
            switch (s) {
                case 0: return REVISION0;
                default: throw new IllegalStateException("Unknown version id: " + s);
            }
        }
    }

    static final byte PREFER_IPV6_MASK = 0b10;

    static byte computeFlags(DcOptions dcOptions) {
        byte flags = 0;
        flags |= dcOptions.isTest() ? TEST_MASK : 0;
        flags |= dcOptions.isPreferIpv6() ? PREFER_IPV6_MASK : 0;
        return flags;
    }

    static final byte TEST_MASK           = 0b000001;
    static final byte STATIC_MASK         = 0b000010;
    static final byte TCPO_ONLY_MASK      = 0b000100;
    static final byte SECRET_MASK         = 0b001000;
    static final byte IPV6_MASK           = 0b010000;
    static final byte THIS_PORT_ONLY_MASK = 0b100000;

    static byte computeFlags(DataCenter dc) {
        byte flags = 0;
        flags |= dc.isTest() ? TEST_MASK : 0;
        flags |= dc.isStatic() ? STATIC_MASK : 0;
        flags |= dc.isTcpObfuscatedOnly() ? TCPO_ONLY_MASK : 0;
        flags |= dc.isThisPortOnly() ? THIS_PORT_ONLY_MASK : 0;
        flags |= dc.isIpv6() ? IPV6_MASK : 0;
        flags |= dc.getSecret().map(b -> SECRET_MASK).orElse((byte) 0);
        return flags;
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
            requireSize(authKeys, "authKeys");
            requireSize(dcOptions.getBackingList(), "dcOptions");
            requireSize(publicRsaKeyRegister.getBackingMap(), "pubRsaKeyRegister");
            this.mainDcId = mainDcId;
            this.selfId = selfId;
            this.authKeys = authKeys;
            this.dcOptions = dcOptions;
            this.publicRsaKeyRegister = publicRsaKeyRegister;
            this.state = state;
        }

        void serialize(ByteBuf buf) {
            buf.writeShortLE(Version.CURRENT.revision);
            buf.writeShortLE(mainDcId);
            buf.writeLongLE(selfId);
            buf.writeShortLE(authKeys.size());
            authKeys.forEach((dcId, authKey) -> {
                buf.writeShortLE(dcId);
                buf.writeLongLE(authKey.getAuthKeyId());
                TlSerialUtil.serializeBytes(buf, authKey.getAuthKey());
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

    static final int MAX_COLLECTIONS_SIZE = 0xffff;

    static void requireSize(Map<?, ?> c, String msg) {
        if (c.size() > MAX_COLLECTIONS_SIZE) {
            throw new IllegalStateException("Too big " + msg + " collection");
        }
    }

    static void requireSize(Collection<?> c, String msg) {
        if (c.size() > MAX_COLLECTIONS_SIZE) {
            throw new IllegalStateException("Too big " + msg + " collection");
        }
    }

    // endregion

    private Settings copySettings() {
        return new Settings(mainDcId, selfId, authKeys, dcOptions, publicRsaKeyRegister, state);
    }

    private boolean isAssociatedToUser() {
        return selfId != 0;
    }

    private Mono<Void> save() {
        return Mono.fromCallable(() -> {
            if (!isAssociatedToUser()) {
                return null;
            }

            if (saving.compareAndSet(false, true)) {
                Settings settings = copySettings();

                if (log.isDebugEnabled())
                    log.debug("Saving information for main DC {} to {}", settings.mainDcId, dataFile);

                ByteBuf data = Unpooled.buffer();
                settings.serialize(data);
                Files.write(dataFile, CryptoUtil.toByteArray(data), StandardOpenOption.WRITE, StandardOpenOption.CREATE);

                saving.set(false);
            }
            return null;
        });
    }

    @Override
    public Mono<Void> initialize() {
        return Mono.fromCallable(() -> {
            if (Files.notExists(dataFile)) {
                return null;
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

                if (log.isDebugEnabled())
                    log.debug("Loaded information for main DC {} from {}", sett.mainDcId, dataFile);
            } catch (Exception e) {
                log.warn("Failed to load information from " + dataFile, e);
            }

            return null;
        });
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
    public Mono<AuthorizationKeyHolder> getAuthKey(DataCenter dc) {
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
                    if (dc.getId() == mainDcId) {
                        return Mono.empty();
                    }

                    this.mainDcId = dc.getId();
                    return save();
                }));
    }

    @Override
    public Mono<Void> updateState(State state) {
        return entityDelegate.updateState(state)
                .publishOn(Schedulers.boundedElastic())
                .and(Mono.defer(() -> {
                    if (state.equals(this.state)) {
                        return Mono.empty();
                    }
                    this.state = state;
                    return save();
                }));
    }

    @Override
    public Mono<Void> updateDcOptions(DcOptions dcOptions) {
        return entityDelegate.updateDcOptions(dcOptions)
                .publishOn(Schedulers.boundedElastic())
                .and(Mono.defer(() -> {
                    if (dcOptions.equals(this.dcOptions)) {
                        return Mono.empty();
                    }
                    this.dcOptions = dcOptions;
                    return save();
                }));
    }

    @Override
    public Mono<Void> updatePublicRsaKeyRegister(PublicRsaKeyRegister publicRsaKeyRegister) {
        return entityDelegate.updatePublicRsaKeyRegister(publicRsaKeyRegister)
                .publishOn(Schedulers.boundedElastic())
                .and(Mono.defer(() -> {
                    if (publicRsaKeyRegister.equals(this.publicRsaKeyRegister)) {
                        return Mono.empty();
                    }
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
    // TODO: persist in Settings

    @Override
    public Mono<Void> onUpdateConfig(Config config) {
        return entityDelegate.onUpdateConfig(config);
    }

    @Override
    public Mono<DataCenter> getWebfileDataCenter() {
        return entityDelegate.getWebfileDataCenter();
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
