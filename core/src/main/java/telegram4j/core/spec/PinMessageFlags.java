package telegram4j.core.spec;

import telegram4j.core.util.BitFlag;
import telegram4j.tl.request.messages.UpdatePinnedMessage;

import static telegram4j.tl.request.messages.UpdatePinnedMessage.*;

/** An enumeration of {@link UpdatePinnedMessage} bit-flags. */
public enum PinMessageFlags implements BitFlag {
    /** Whether need to pin message silently, without triggering a notification. */
    SILENT(SILENT_POS),

    /** Whether message should be unpinned. */
    UNPIN(UNPIN_POS),

    /** Whether message should only be pinned for the <i>current</i> user in private messages. */
    PM_ONE_SIDE(PM_ONESIDE_POS);

    private final byte position;

    PinMessageFlags(byte position) {
        this.position = position;
    }

    @Override
    public byte position() {
        return position;
    }
}
