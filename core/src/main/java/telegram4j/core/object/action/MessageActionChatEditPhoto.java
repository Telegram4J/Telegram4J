package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Photo;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BasePhoto;
import telegram4j.tl.InputPeer;
import telegram4j.tl.InputPeerEmpty;

import java.util.Objects;
import java.util.Optional;

public class MessageActionChatEditPhoto extends BaseMessageAction {

    @Nullable
    private final telegram4j.tl.MessageActionChatEditPhoto data;
    private final InputPeer peer;
    private final int messageId;

    public MessageActionChatEditPhoto(MTProtoTelegramClient client) {
        super(client, Type.DELETE_CHAT_PHOTO);

        this.data = null;
        this.peer = InputPeerEmpty.instance();
        this.messageId = -1;
    }

    public MessageActionChatEditPhoto(MTProtoTelegramClient client, telegram4j.tl.MessageActionChatEditPhoto data,
                                      InputPeer peer, int messageId) {
        super(client, Type.EDIT_CHAT_PHOTO);
        this.data = Objects.requireNonNull(data, "data");
        this.peer = Objects.requireNonNull(peer, "peer");
        this.messageId = messageId;
    }

    public Optional<Photo> getCurrentPhoto() {
        return Optional.ofNullable(data)
                .map(d -> TlEntityUtil.unmapEmpty(d.photo(), BasePhoto.class))
                .map(d -> new Photo(client, d, messageId, peer));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MessageActionChatEditPhoto that = (MessageActionChatEditPhoto) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), data);
    }

    @Override
    public String toString() {
        return "MessageActionChatEditPhoto{" +
                "type=" + type +
                ", data=" + data +
                '}';
    }
}
