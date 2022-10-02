package telegram4j.core.spec;

import telegram4j.core.util.BitFlag;

import static telegram4j.tl.request.messages.UpdatePinnedMessage.*;

public enum PinMessageFlags implements BitFlag {
    UNPIN(UNPIN_POS),
    SILENT(SILENT_POS),
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
