package telegram4j.core.handle;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.util.Id;
import telegram4j.tl.InputPeer;
import telegram4j.tl.InputUser;
import telegram4j.tl.Peer;

public abstract class MTProtoHandle extends EntityHandle {

    protected MTProtoHandle(MTProtoTelegramClient client) {
        super(client);
    }

    protected long rawIdOf(InputUser inputUser) {
        return Id.of(inputUser, client.getSelfId()).asLong();
    }

    protected Peer toPeer(InputPeer inputPeer) {
        return Id.of(inputPeer, client.getSelfId()).asPeer();
    }
}
