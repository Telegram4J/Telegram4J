package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.tl.ChatBannedRights;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;

import static telegram4j.tl.ChatBannedRights.*;

public class ChatBannedRightsSettings {
    private final ChatBannedRights data;

    public ChatBannedRightsSettings(ChatBannedRights data) {;
        this.data = Objects.requireNonNull(data);
    }

    public EnumSet<Right> getRights() {
        return Right.of(data);
    }

    public Instant getUntilTimestamp() {
        return Instant.ofEpochSecond(data.untilDate());
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatBannedRightsSettings that = (ChatBannedRightsSettings) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "ChatBannedRightsSettings{" +
                "data=" + data +
                '}';
    }

    /** An enumeration of permissions that describes disallowed actions. */
    public enum Right implements BitFlag {

        /** Disallows to view messages in a channel/chat. */
        VIEW_MESSAGES(VIEW_MESSAGES_POS),

        /** Disallows to send messages in a channel/chat. */
        SEND_MESSAGES(SEND_MESSAGES_POS),

        /** Disallows to send any media in a channel/chat. */
        SEND_MEDIA(SEND_MEDIA_POS),

        /** Disallows to send stickers in a channel/chat. */
        SEND_STICKERS(SEND_STICKERS_POS),

        /** Disallows to send gifs in a channel/chat. */
        SEND_GIFS(SEND_GIFS_POS),

        /** Disallows to send games in a channel/chat. */
        SEND_GAMES(SEND_GAMES_POS),

        /** Disallows to use inline bots in a channel/chat. */
        SEND_INLINE(SEND_INLINE_POS),

        /** Disallows to embed links in the messages of a channel/chat. */
        EMBED_LINKS(EMBED_LINKS_POS),

        /** Disallows to send polls in a channel/chat. */
        SEND_POLLS(SEND_POLLS_POS),

        /** Disallows to change the description in a channel/chat. */
        CHANGE_INFO(CHANGE_INFO_POS),

        /** Disallows to invite users in a channel/chat. */
        INVITE_USERS(INVITE_USERS_POS),

        /** Disallows to pin messages in a channel/chat. */
        PIN_MESSAGES(PIN_MESSAGES_POS);

        private final byte position;

        Right(byte position) {
            this.position = position;
        }

        @Override
        public byte position() {
            return position;
        }

        /**
         * Computes {@link EnumSet} from raw {@link ChatBannedRights} data.
         *
         * @param data The raw chat banned rights data.
         * @return The {@link EnumSet} of the mapped {@link ChatBannedRights} flags.
         */
        public static EnumSet<Right> of(ChatBannedRights data) {
            var set = EnumSet.allOf(Right.class);
            int flags = data.flags();
            set.removeIf(value -> (flags & value.mask()) == 0);
            return set;
        }
    }
}
