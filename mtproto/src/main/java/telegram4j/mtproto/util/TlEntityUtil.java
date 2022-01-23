package telegram4j.mtproto.util;

import reactor.util.annotation.Nullable;
import telegram4j.tl.*;
import telegram4j.tl.api.EmptyObject;
import telegram4j.tl.api.TlObject;

public class TlEntityUtil {

    private TlEntityUtil() {
    }

    public static String stripUsername(String username) {
        return username.toLowerCase().trim()
                .replace(".", "")
                .replace("@", "");
    }

    public static long getRawPeerId(Peer peer) {
        switch (peer.identifier()) {
            case PeerChannel.ID: return ((PeerChannel) peer).channelId();
            case PeerChat.ID: return ((PeerChat) peer).chatId();
            case PeerUser.ID: return ((PeerUser) peer).userId();
            default: throw new IllegalArgumentException("Unknown peer type: " + peer);
        }
    }

    public static InputPeer toInputPeer(InputUser user) {
        switch (user.identifier()) {
            case InputUserFromMessage.ID:
                InputUserFromMessage d = (InputUserFromMessage) user;
                return ImmutableInputPeerUserFromMessage.of(d.peer(), d.msgId(), d.userId());
            case InputUserSelf.ID: return InputPeerSelf.instance();
            case BaseInputUser.ID:
                BaseInputUser v = (BaseInputUser) user;
                return ImmutableInputPeerUser.of(v.userId(), v.accessHash());
            default: throw new IllegalArgumentException("Unknown input user type: " + user);
        }
    }

    public static InputPeer toInputPeer(InputChannel channel) {
        switch (channel.identifier()) {
            case InputChannelFromMessage.ID:
                InputChannelFromMessage d = (InputChannelFromMessage) channel;
                return ImmutableInputPeerChannelFromMessage.of(d.peer(), d.msgId(), d.channelId());
            case BaseInputChannel.ID:
                BaseInputChannel v = (BaseInputChannel) channel;
                return ImmutableInputPeerChannel.of(v.channelId(), v.accessHash());
            default: throw new IllegalArgumentException("Unknown input channel type: " + channel);
        }
    }

    public static InputUser toInputUser(InputPeer peer) {
        switch (peer.identifier()) {
            case InputPeerUserFromMessage.ID:
                InputPeerUserFromMessage d = (InputPeerUserFromMessage) peer;
                return ImmutableInputUserFromMessage.of(d.peer(), d.msgId(), d.userId());
            case InputPeerSelf.ID: return InputUserSelf.instance();
            case InputPeerUser.ID:
                InputPeerUser v = (InputPeerUser) peer;
                return ImmutableBaseInputUser.of(v.userId(), v.accessHash());
            default: throw new IllegalArgumentException("Unknown input peer user type: " + peer);
        }
    }

    public static InputChannel toInputChannel(InputPeer peer) {
        switch (peer.identifier()) {
            case InputPeerChannelFromMessage.ID:
                InputPeerChannelFromMessage d = (InputPeerChannelFromMessage) peer;
                return ImmutableInputChannelFromMessage.of(d.peer(), d.msgId(), d.channelId());
            case InputPeerChannel.ID:
                InputPeerChannel v = (InputPeerChannel) peer;
                return ImmutableBaseInputChannel.of(v.channelId(), v.accessHash());
            default: throw new IllegalArgumentException("Unknown input peer channel type: " + peer);
        }
    }

    @Nullable
    public static <T extends TlObject, R extends T> R unmapEmpty(@Nullable T obj, Class<R> impl) {
        if (obj == null || obj instanceof EmptyObject) {
            return null;
        }
        return impl.cast(obj);
    }
}
