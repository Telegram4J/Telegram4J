package telegram4j.core.object.action;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Photo;
import telegram4j.mtproto.util.TlEntityUtil;
import telegram4j.tl.BasePhoto;
import telegram4j.tl.InputPeer;

import java.util.Objects;
import java.util.Optional;

public class MessageActionChatEditPhoto extends BaseMessageAction {

    private final telegram4j.tl.MessageActionChatEditPhoto data;
    private final InputPeer peer;
    private final int messageId;

    public MessageActionChatEditPhoto(MTProtoTelegramClient client, telegram4j.tl.MessageActionChatEditPhoto data,
                                      InputPeer peer, int messageId) {
        super(client, Type.CHAT_EDIT_PHOTO);
        this.data = Objects.requireNonNull(data, "data");
        this.peer = Objects.requireNonNull(peer, "peer");
        this.messageId = messageId;
    }

    public Optional<Photo> getPhoto() {
        return Optional.ofNullable(TlEntityUtil.unmapEmpty(data.photo(), BasePhoto.class))
                .map(d -> new Photo(getClient(), d, peer, messageId));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageActionChatEditPhoto that = (MessageActionChatEditPhoto) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageActionChatEditPhoto{" +
                "data=" + data +
                '}';
    }
}
