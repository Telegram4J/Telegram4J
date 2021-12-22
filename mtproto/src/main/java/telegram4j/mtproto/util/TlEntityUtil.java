package telegram4j.mtproto.util;

import reactor.util.annotation.Nullable;
import telegram4j.tl.Peer;
import telegram4j.tl.PeerChannel;
import telegram4j.tl.PeerChat;
import telegram4j.tl.PeerUser;
import telegram4j.tl.api.EmptyObject;
import telegram4j.tl.api.TlObject;

import java.util.function.Function;

public class TlEntityUtil {

    private static final long ZERO_CHANNEL_ID = -1000000000000L;

    private TlEntityUtil() {
    }

    public static long getPeerId(Peer peer) {
        switch (peer.identifier()) {
            case PeerChannel.ID:
                PeerChannel peerChannel = (PeerChannel) peer;
                return ZERO_CHANNEL_ID - peerChannel.channelId();
            case PeerChat.ID:
                PeerChat peerChat = (PeerChat) peer;
                return -peerChat.chatId();
            case PeerUser.ID:
                PeerUser peerUser = (PeerUser) peer;
                return peerUser.userId();
            default: throw new IllegalArgumentException("Unknown peer type: " + peer);
        }
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
    public static <T extends TlObject, R extends T> R unmapEmpty(T obj, Class<R> impl) {
        if (obj instanceof EmptyObject) {
            return null;
        }
        return impl.cast(obj);
    }

    public static <F, S, R> R or(@Nullable F first, @Nullable S second,
                                 Function<F, ? extends R> firstMap,
                                 Function<S, ? extends R> secondMap) {
        if (first != null) {
            return firstMap.apply(first);
        }
        if (second != null) {
            return secondMap.apply(second);
        }
        throw new IllegalArgumentException("All sides are null.");
    }
}
