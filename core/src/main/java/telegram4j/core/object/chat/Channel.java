package telegram4j.core.object.chat;

import telegram4j.core.object.BotInfo;
import telegram4j.core.object.PeerId;
import telegram4j.core.object.RestrictionReason;
import telegram4j.core.object.StickerSet;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface Channel extends Chat {

    /**
     * Gets title (i.e. name) of channel.
     *
     * @return The name of channel.
     */
    String getTitle();

    /**
     * Gets username of channel, if present. This username can be used to retrieve channel
     * via {@link telegram4j.core.retriever.EntityRetriever#resolvePeer(PeerId)}
     * or used in {@link PeerId peer id}.
     *
     *
     * @return
     */
    Optional<String> getUsername();

    /**
     * Gets information about chat, if present.
     *
     * @return The information about chat, if present.
     */
    Optional<String> getAbout();

    /**
     * Gets list of information about chat bots, if present.
     *
     * @return The list of information about chat bots, if present.
     */
    Optional<List<BotInfo>> getBotInfo();

    /**
     * Gets timestamp when <i>current</i> user joined, or if the user isn't a member, channel created.
     *
     * @return The timestamp when <i>current</i> user joined, or if the user isn't a member, channel created.
     */
    Instant getCreateTimestamp();

    /**
     * Gets associated with this channel sticker set, if present.
     *
     * @return The associated sticker set, if present.
     */
    Optional<StickerSet> getStickerSet();

    Optional<List<RestrictionReason>> getRestrictionReason();
}
