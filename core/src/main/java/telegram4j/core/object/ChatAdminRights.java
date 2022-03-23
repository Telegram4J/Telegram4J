package telegram4j.core.object;

import java.util.EnumSet;

public enum ChatAdminRights {

    /** If set, allows the admin to modify the description of the <a href="https://core.telegram.org/api/channel">channel/supergroup</a>. */
    CHANGE_INFO(0),

    /** If set, allows the admin to post messages in the <a href="https://core.telegram.org/api/channel">channel</a>. */
    POST_MESSAGES(1),

    /** If set, allows the admin to also edit messages from other admins in the <a href="https://core.telegram.org/api/channel">channel</a>. */
    EDIT_MESSAGES(2),

    /** If set, allows the admin to also delete messages from other admins in the <a href="https://core.telegram.org/api/channel">channel</a>. */
    DELETE_MESSAGES(3),

    /** If set, allows the admin to ban users from the <a href="https://core.telegram.org/api/channel">channel/supergroup</a> */
    BAN_USERS(4),

    /** If set, allows the admin to invite users in the <a href="https://core.telegram.org/api/channel">channel/supergroup</a>. */
    INVITE_USERS(5),

    /** If set, allows the admin to pin messages in the <a href="https://core.telegram.org/api/channel">channel/supergroup</a>. */
    PIN_MESSAGES(7),

    /**
     * If set, allows the admin to add other admins with the same (or more limited)
     * permissions in the <a href="https://core.telegram.org/api/channel">channel/supergroup</a>.
     */
    ADD_ADMINS(9),

    /** Whether this admin is anonymous. */
    ANONYMOUS(10),

    /** If set, allows the admin to change group call/livestream settings. */
    MANAGE_CALL(11),

    /** Set this flag if none of the other flags are set, but you still want the user to be an admin. */
    OTHER(12);

    private final int value;
    private final int flag;

    ChatAdminRights(int value) {
        this.value = value;
        this.flag = 1 << value;
    }

    /**
     * Gets flag position, used in the {@link #getFlag()} as {@code 1 << position}.
     *
     * @return The flag shift position.
     */
    public int getValue() {
        return value;
    }

    /**
     * Gets bit-mask for flag.
     *
     * @return The bit-mask for flag.
     */
    public int getFlag() {
        return flag;
    }

    /**
     * Computes {@link EnumSet} from raw {@link telegram4j.tl.ChatAdminRights} data.
     *
     * @param chatAdminRights The chat admin rights data.
     * @return The {@link EnumSet} of the {@link telegram4j.tl.ChatAdminRights} flags.
     */
    public static EnumSet<ChatAdminRights> of(telegram4j.tl.ChatAdminRights chatAdminRights) {
        EnumSet<ChatAdminRights> set = EnumSet.noneOf(ChatAdminRights.class);
        int flags = chatAdminRights.flags();
        for (ChatAdminRights value : values()) {
            if ((flags & value.flag) != 0) {
                set.add(value);
            }
        }
        return set;
    }
}
