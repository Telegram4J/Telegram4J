package telegram4j.core.object;

import telegram4j.core.object.chat.Channel;
import telegram4j.core.retriever.EntityRetriever;
import telegram4j.core.util.PeerId;

import java.util.List;
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

    // TODO: map bit-flags?
    /**
     * Gets list of another associated to this peer usernames, if present, otherwise will return empty list.
     *
     * @return The list of another associated to this peer usernames, if present, otherwise will return empty list.
     */
    List<telegram4j.tl.Username> getUsernames();
}
