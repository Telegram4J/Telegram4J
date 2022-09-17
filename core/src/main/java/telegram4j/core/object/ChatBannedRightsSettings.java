package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.ChatBannedRights;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;

import static telegram4j.tl.ChatBannedRights.*;

public class ChatBannedRightsSettings implements TelegramObject {
    private final MTProtoTelegramClient client;
    private final ChatBannedRights data;

    public ChatBannedRightsSettings(MTProtoTelegramClient client, ChatBannedRights data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
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

    public enum Right implements BitFlag {

        /** If set, does not allow a user to view messages in a <a href="https://core.telegram.org/api/channel">supergroup/channel/chat</a>. */
        VIEW_MESSAGES(VIEW_MESSAGES_POS),

        /** If set, does not allow a user to send messages in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        SEND_MESSAGES(SEND_MESSAGES_POS),

        /** If set, does not allow a user to send any media in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        SEND_MEDIA(SEND_MEDIA_POS),

        /** If set, does not allow a user to send stickers in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        SEND_STICKERS(SEND_STICKERS_POS),

        /** If set, does not allow a user to send gifs in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        SEND_GIFS(SEND_GIFS_POS),

        /** If set, does not allow a user to send games in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        SEND_GAMES(SEND_GAMES_POS),

        /** If set, does not allow a user to use inline bots in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        SEND_INLINE(SEND_INLINE_POS),

        /** If set, does not allow a user to embed links in the messages of a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        EMBED_LINKS(EMBED_LINKS_POS),

        /** If set, does not allow a user to send polls in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        SEND_POLLS(SEND_POLLS_POS),

        /** If set, does not allow any user to change the description of a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        CHANGE_INFO(CHANGE_INFO_POS),

        /** If set, does not allow any user to invite users in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        INVITE_USERS(INVITE_USERS_POS),

        /** If set, does not allow any user to pin messages in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
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
         * @return The {@link EnumSet} of the {@link ChatBannedRights} flags.
         */
        public static EnumSet<Right> of(ChatBannedRights data) {
            var set = EnumSet.allOf(Right.class);
            int flags = data.flags();
            set.removeIf(value -> (flags & value.mask()) == 0);
            return set;
        }
    }
}
