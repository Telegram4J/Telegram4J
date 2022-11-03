package telegram4j.mtproto.file;

import io.netty.buffer.ByteBuf;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.*;
import telegram4j.tl.messages.StickerSet;

import java.util.Objects;

public abstract class Context {

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
        STICKER_SET;

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
     * Gets common instance for documents with empty context.
     *
     * @return The common instance for documents with empty context.
     */
    public static Context noOpContext() {
        return noOpContextInstance;
    }

    static Context deserialize(ByteBuf buf, Context.Type type) {
        switch (type) {
            case STICKER_SET:
                InputStickerSet stickerSet = TlDeserializer.deserialize(buf);
                return new StickerSetContext(stickerSet);
            case MESSAGE_MEDIA: {
                Peer peer = TlDeserializer.deserialize(buf);
                int messageId = buf.readIntLE();
                return new MessageMediaContext(peer, messageId);
            }
            case BOT_INFO:
                Peer chatPeer = TlDeserializer.deserialize(buf);
                long botId = buf.isReadable(8) ? buf.readLongLE() : -1;
                return new BotInfoContext(chatPeer, botId);
            case PROFILE_PHOTO: {
                InputPeer peer = TlDeserializer.deserialize(buf);
                return new ProfilePhotoContext(peer);
            }
            case CHAT_PHOTO:
                InputPeer peer = TlDeserializer.deserialize(buf);
                int messageId = buf.isReadable(4) ? buf.readIntLE() : -1;
                return new ChatPhotoContext(peer, messageId);
            default: throw new IllegalStateException();
        }
    }

    private static final Context noOpContextInstance;

    static {
        noOpContextInstance = new Context() {
            @Override
            public Type getType() { return Type.UNKNOWN; }
            @Override
            void serialize(ByteBuf buf) {}
        };
    }
}
