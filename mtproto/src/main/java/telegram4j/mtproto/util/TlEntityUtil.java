package telegram4j.mtproto.util;

import reactor.util.annotation.Nullable;
import telegram4j.tl.Peer;
import telegram4j.tl.PeerChannel;
import telegram4j.tl.PeerChat;
import telegram4j.tl.PeerUser;
import telegram4j.tl.api.EmptyObject;
import telegram4j.tl.api.TlObject;

public class TlEntityUtil {

    private TlEntityUtil() {
    }

    public static long getRawPeerId(Peer peer) {
        switch (peer.identifier()) {
            case PeerChannel.ID: return ((PeerChannel) peer).channelId();
            case PeerChat.ID: return ((PeerChat) peer).chatId();
            case PeerUser.ID: return ((PeerUser) peer).userId();
            default: throw new IllegalArgumentException("Unknown peer type: " + peer);
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
