package telegram4j.core.object;

import telegram4j.core.util.Id;

/** An object that can be an author or a chat from where messages are sent. */
public interface PeerEntity extends TelegramObject {

    /**
     * Gets the peer identifier of this entity.
     *
     * @return The peer identifier of this entity.
     */
    Id getId();
}
