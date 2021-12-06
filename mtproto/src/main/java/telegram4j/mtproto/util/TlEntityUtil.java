package telegram4j.mtproto.util;

import telegram4j.tl.Peer;
import telegram4j.tl.PeerChannel;
import telegram4j.tl.PeerChat;
import telegram4j.tl.PeerUser;

public class TlEntityUtil {

    private TlEntityUtil() {}

    public static long peerId(Peer peer) {
        switch (peer.identifier()) {
            case PeerChannel.ID: return ((PeerChannel) peer).channelId();
            case PeerChat.ID: return ((PeerChat) peer).chatId();
            case PeerUser.ID: return ((PeerUser) peer).userId();
            default: throw new IllegalArgumentException("Unknown peer type: " + peer);
        }
    }
}
