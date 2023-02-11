package telegram4j.mtproto.file;

import io.netty.buffer.ByteBuf;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.messages.StickerSet;

import java.util.Objects;

public sealed abstract class Context
        permits BotInfoContext, MessageActionContext,
                MessageMediaContext, ProfilePhotoContext, StickerSetContext,
                Context.NoOpContext {

    /**
     * Gets type of context.
     *
     * @return The type of context.
     */
    public abstract Type getType();

    abstract void serialize(ByteBuf buf);

    public enum Type {
        UNKNOWN, // no context
        MESSAGE_MEDIA, // from any MessageMedia; detected by documentType
        BOT_INFO, // UserFull#botInfo() or ChatFull#botInfo()
        PROFILE_PHOTO, // BaseUser#photo() and other
        CHAT_PHOTO,  // UserFull#profilePhoto() and other
        STICKER_SET,
        MESSAGE_ACTION; // MessageActionSuggestProfilePhoto

        static final Type[] ALL = values();
    }

    /**
     * Creates new context which corresponding to {@link MessageMedia} object.
     *
     * @throws IllegalArgumentException if specified message id is negative.
     * @param chatPeer The id of chat peer where media was found.
     * @param messageId The id of message where media was found.
     * @return A new {@code MessageMediaContext} context.
     */
    public static MessageMediaContext createMediaContext(Peer chatPeer, int messageId) {
        Objects.requireNonNull(chatPeer);
        if (messageId < 0)
            throw new IllegalArgumentException("Message id must be positive");
        return new MessageMediaContext(chatPeer, messageId);
    }

    /**
     * Creates new context which contains user {@link InputPeer} for user photos.
     *
     * @throws IllegalArgumentException if specified peer isn't {@link TlEntityUtil#isUserPeer(InputPeer) user peer}.
     * @param peer The id of user.
     * @return A new {@code ProfilePhotoContext} context.
     */
    public static ProfilePhotoContext createUserPhotoContext(InputPeer peer) { // TODO: or use InputUser type?
        if (!TlEntityUtil.isUserPeer(peer))
            throw new IllegalArgumentException("Unexpected InputPeer type: " + peer);
        return new ProfilePhotoContext(peer);
    }

    /**
     * Creates new context which corresponding to {@link MessageActionChatEditPhoto} object or
     * {@link UserFull#profilePhoto() full photos}.
     *
     * @param peer The id of chat where {@link BotInfo} was found.
     * @param messageId The id of message where this chat photo was found,
     * if it from {@link MessageActionChatEditPhoto}, otherwise -1.
     * @return A new {@code ChatPhotoContext} context.
     */
    public static ChatPhotoContext createChatPhotoContext(InputPeer peer, int messageId) {
        if (peer == InputPeerEmpty.instance())
            throw new IllegalArgumentException("Unexpected InputPeer type");
        return new ChatPhotoContext(peer, messageId);
    }

    /**
     * Creates new context which corresponding to {@link BotInfo} object.
     *
     * @param peer The id of chat where {@link BotInfo} was found.
     * @param botId The id of bot from {@link BotInfo#userId()}.
     * @return A new {@code BotInfoContext} context.
     */
    public static BotInfoContext createBotInfoContext(Peer peer, long botId) {
        Objects.requireNonNull(peer);
        if (botId < 0)
            throw new IllegalArgumentException("Invalid bot id");
        if (ImmutablePeerUser.of(botId).equals(peer)) // TODO: verify
            botId = -1;
        return new BotInfoContext(peer, botId);
    }

    /**
     * Creates new context which corresponding to {@link StickerSet} object.
     *
     * @param stickerSet The id of sticker set where sticker was found.
     * @return A new {@code StickerSetContext} context.
     */
    public static StickerSetContext createStickerSetContext(InputStickerSet stickerSet) {
        if (stickerSet == InputStickerSetEmpty.instance())
            throw new IllegalArgumentException();
        return new StickerSetContext(stickerSet);
    }

    /**
     * Creates new context which corresponding to {@link MessageAction} object.
     *
     * @throws IllegalArgumentException if specified message id is negative.
     * @param chatPeer The id of chat peer where media was found.
     * @param messageId The id of message where media was found.
     * @return A new {@code MessageActionContext} context.
     */
    public static MessageActionContext createActionContext(Peer chatPeer, int messageId) {
        Objects.requireNonNull(chatPeer);
        if (messageId < 0)
            throw new IllegalArgumentException("Message id must be positive");
        return new MessageActionContext(chatPeer, messageId);
    }

    /**
     * Gets common instance for documents with empty context.
     *
     * @return The common instance for documents with empty context.
     */
    public static Context noOpContext() {
        return NoOpContext.instance;
    }

    static Context deserialize(ByteBuf buf, Context.Type type) {
        return switch (type) {
            case STICKER_SET -> {
                InputStickerSet stickerSet = TlDeserializer.deserialize(buf);
                yield new StickerSetContext(stickerSet);
            }
            case MESSAGE_MEDIA -> {
                Peer peer = deserializePeer(buf);
                int messageId = buf.readIntLE();
                yield new MessageMediaContext(peer, messageId);
            }
            case MESSAGE_ACTION -> {
                Peer peer = deserializePeer(buf);
                int messageId = buf.readIntLE();
                yield new MessageActionContext(peer, messageId);
            }
            case BOT_INFO -> {
                Peer chatPeer = deserializePeer(buf);
                long botId = buf.readByte() != 0 ? buf.readLongLE() : -1;
                yield new BotInfoContext(chatPeer, botId);
            }
            case PROFILE_PHOTO -> {
                InputPeer peer = deserializeInputPeer(buf);
                yield new ProfilePhotoContext(peer);
            }
            case CHAT_PHOTO -> {
                InputPeer peer = deserializeInputPeer(buf);
                int messageId = buf.readByte() != 0 ? buf.readIntLE() : -1;
                yield new ChatPhotoContext(peer, messageId);
            }
            default -> throw new IllegalStateException();
        };
    }

    static final byte PEER_USER = 0;
    static final byte PEER_CHAT = 1;
    static final byte PEER_CHANNEL = 2;

    static final byte I_PEER_CHANNEL = 0;
    static final byte I_PEER_CHANNEL_MIN = 1;
    static final byte I_PEER_CHAT = 2;
    static final byte I_PEER_USER = 3;
    static final byte I_PEER_USER_MIN = 4;
    static final byte I_PEER_SELF = 5;

    static Peer deserializePeer(ByteBuf buf) {
        byte type = buf.readByte();
        return switch (type) {
            case PEER_CHANNEL -> ImmutablePeerChannel.of(buf.readLongLE());
            case PEER_USER -> ImmutablePeerUser.of(buf.readLongLE());
            case PEER_CHAT -> ImmutablePeerChat.of(buf.readLongLE());
            default -> throw new IllegalArgumentException("Unknown Peer type: " + type);
        };
    }

    static InputPeer deserializeInputPeer(ByteBuf buf) {
        byte type = buf.readByte();
        return switch (type) {
            case I_PEER_CHANNEL -> {
                long channelId = buf.readLongLE();
                long accessHash = buf.readLongLE();
                yield ImmutableInputPeerChannel.of(channelId, accessHash);
            }
            case I_PEER_CHANNEL_MIN -> {
                long channelId = buf.readLongLE();
                int msgId = buf.readIntLE();
                InputPeer peer = deserializeInputPeer(buf);
                yield ImmutableInputPeerChannelFromMessage.of(peer, msgId, channelId);
            }
            case I_PEER_CHAT -> ImmutableInputPeerChat.of(buf.readLongLE());
            case I_PEER_USER -> {
                long userId = buf.readLongLE();
                long accessHash = buf.readLongLE();
                yield ImmutableInputPeerUser.of(userId, accessHash);
            }
            case I_PEER_USER_MIN -> {
                long userId = buf.readLongLE();
                int msgId = buf.readIntLE();
                InputPeer peer = deserializeInputPeer(buf);
                yield ImmutableInputPeerUserFromMessage.of(peer, msgId, userId);
            }
            case I_PEER_SELF -> InputPeerSelf.instance();
            default -> throw new IllegalArgumentException("Unknown InputPeer type: " + type);
        };
    }

    static void serializeInputPeer(ByteBuf buf, InputPeer peer) {
        switch (peer.identifier()) {
            case InputPeerChannel.ID -> {
                var ipch = (InputPeerChannel) peer;
                buf.writeByte(I_PEER_CHANNEL);
                buf.writeLongLE(ipch.channelId());
                buf.writeLongLE(ipch.accessHash());
            }
            case InputPeerChannelFromMessage.ID -> {
                var ipchm = (InputPeerChannelFromMessage) peer;
                buf.writeByte(I_PEER_CHANNEL_MIN);
                buf.writeLongLE(ipchm.channelId());
                buf.writeIntLE(ipchm.msgId());
                serializeInputPeer(buf, ipchm.peer());
            }
            case InputPeerChat.ID -> {
                var ipc = (InputPeerChat) peer;
                buf.writeByte(I_PEER_CHAT);
                buf.writeLongLE(ipc.chatId());
            }
            case InputPeerSelf.ID -> buf.writeByte(I_PEER_SELF);
            case InputPeerUser.ID -> {
                var ipu = (InputPeerUser) peer;
                buf.writeByte(I_PEER_USER);
                buf.writeLongLE(ipu.userId());
                buf.writeLongLE(ipu.accessHash());
            }
            case InputPeerUserFromMessage.ID -> {
                var ipum = (InputPeerUserFromMessage) peer;
                buf.writeByte(I_PEER_USER_MIN);
                buf.writeLongLE(ipum.userId());
                buf.writeIntLE(ipum.msgId());
                serializeInputPeer(buf, ipum.peer());
            }
            default -> throw new IllegalArgumentException("Unexpected type of InputPeer: " + peer);
        }
    }

    static void serializePeer(ByteBuf buf, Peer peer) {
        switch (peer.identifier()) {
            case PeerUser.ID -> {
                var pu = (PeerUser) peer;
                buf.writeByte(PEER_USER);
                buf.writeLongLE(pu.userId());
            }
            case PeerChat.ID -> {
                var pc = (PeerChat) peer;
                buf.writeByte(PEER_CHAT);
                buf.writeLongLE(pc.chatId());
            }
            case PeerChannel.ID -> {
                var pch = (PeerChannel) peer;
                buf.writeByte(PEER_CHANNEL);
                buf.writeLongLE(pch.channelId());
            }
            default -> throw new IllegalArgumentException("Unknown peer type: " + peer);
        }
    }

    static final class NoOpContext extends Context {
        static final NoOpContext instance = new NoOpContext();

        NoOpContext() {}

        @Override
        public Type getType() { return Type.UNKNOWN; }

        @Override
        void serialize(ByteBuf buf) {}
    }
}
