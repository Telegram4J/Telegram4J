package telegram4j.core.object;

import telegram4j.core.MTProtoTelegramClient;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

public class PeerSettings implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.PeerSettings data;

    public PeerSettings(MTProtoTelegramClient client, telegram4j.tl.PeerSettings data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public EnumSet<Flag> getFlags() {
        return Flag.of(data);
    }

    public Optional<Integer> getGeoDistance() {
        return Optional.ofNullable(data.geoDistance());
    }

    public enum Flag {

        /** Whether we can still report the user for spam. */
        REPORT_SPAM(0),

        /** Whether we can add the user as contact. */
        ADD_CONTACT(1),

        /** Whether we can block the user. */
        BLOCK_CONTACT(2),

        /** Whether we can share the user's contact. */
        SHARE_CONTACT(3),

        /** Whether a special exception for contacts is needed. */
        NEED_CONTACTS_EXCEPTION(4),

        /** Whether we can report a geogroup is irrelevant for this location. */
        REPORT_GEO(5),

        /**
         * Whether this peer was automatically archived according
         * to <a href="https://core.telegram.org/constructor/globalPrivacySettings">privacy settings</a>.
         */
        AUTOARCHIVED(7),

        /** Whether we can invite members to a <a href="https://core.telegram.org/api/channel">group or channel</a>. */
        INVITE_MEMBERS(8);

        private final int value;
        private final int flag;

        Flag(int value) {
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
         * Computes {@link EnumSet} from raw {@link telegram4j.tl.PeerSettings} data.
         *
         * @param data The message data.
         * @return The {@link EnumSet} of the {@link telegram4j.tl.PeerSettings} flags.
         */
        public static EnumSet<Flag> of(telegram4j.tl.PeerSettings data) {
            EnumSet<Flag> set = EnumSet.noneOf(Flag.class);
            int flags = data.flags();
            for (Flag value : values()) {
                if ((flags & value.flag) != 0) {
                    set.add(value);
                }
            }
            return set;
        }
    }
}
