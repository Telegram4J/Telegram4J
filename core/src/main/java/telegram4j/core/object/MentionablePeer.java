package telegram4j.core.object;

import telegram4j.core.object.chat.Channel;
import telegram4j.core.retriever.EntityRetriever;
import telegram4j.core.util.PeerId;

import java.util.Optional;

/** Interface for {@link User} and {@link Channel} peers which can have username. */
public interface MentionablePeer extends PeerEntity {

    /**
     * Gets username of this user in format <b>username</b>, if present.
     * Can be used in the {@link EntityRetriever#resolvePeer(PeerId)}
     *
     * @return The username of this user, if present.
     */
    Optional<String> getUsername();
}
