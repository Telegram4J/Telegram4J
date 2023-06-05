package telegram4j.core.object;

import telegram4j.tl.RestrictionReason;

import java.util.List;

public sealed interface Restrictable extends TelegramObject
        permits Message, MentionablePeer {

    /**
     * Gets immutable list of {@link RestrictionReason} for why access to this entity must be restricted,
     * if message is not service and list present.
     *
     * @return The immutable list of the {@link RestrictionReason}, if present otherwise empty list.
     */
    List<RestrictionReason> getRestrictionReasons();
}
