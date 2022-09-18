package telegram4j.core.util;

import telegram4j.core.object.chat.UnavailableChatException;
import telegram4j.tl.ChannelForbidden;
import telegram4j.tl.ChatEmpty;
import telegram4j.tl.ChatForbidden;

/**
 * An enum that denotes the action required to be performed
 * when receiving {@link ChatForbidden}/{@link ChannelForbidden} objects.
 * <p>
 * These policies don't handle {@link ChatEmpty} objects, they would always be mapped as {@code null}.
 */
public enum UnavailableChatPolicy {
    /** An action indicating mapping these objects to {@code null}. */
    NULL_MAPPING,

    /** An action indicating throwing an {@link UnavailableChatException}. */
    THROWING
}
