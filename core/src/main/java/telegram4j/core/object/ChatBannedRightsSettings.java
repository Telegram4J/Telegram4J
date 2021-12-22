package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.ChatBannedRights;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;

public class ChatBannedRightsSettings implements TelegramObject {
    private final MTProtoTelegramClient client;
    private final ChatBannedRights data;

    public ChatBannedRightsSettings(MTProtoTelegramClient client, ChatBannedRights data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
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

    public enum Right {

        /** If set, does not allow a user to view messages in a <a href="https://core.telegram.org/api/channel">supergroup/channel/chat</a>. */
        VIEW_MESSAGES(0),

        /** If set, does not allow a user to send messages in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        SEND_MESSAGES(1),

        /** If set, does not allow a user to send any media in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        SEND_MEDIA(2),

        /** If set, does not allow a user to send stickers in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        SEND_STICKERS(3),

        /** If set, does not allow a user to send gifs in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        SEND_GIFS(4),

        /** If set, does not allow a user to send games in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        SEND_GAMES(5),

        /** If set, does not allow a user to use inline bots in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        SEND_INLINE(6),

        /** If set, does not allow a user to embed links in the messages of a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        EMBED_LINKS(7),

        /** If set, does not allow a user to send polls in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        SEND_POLLS(8),

        /** If set, does not allow any user to change the description of a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        CHANGE_INFO(10),

        /** If set, does not allow any user to invite users in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        INVITE_USERS(15),

        /** If set, does not allow any user to pin messages in a <a href="https://core.telegram.org/api/channel">supergroup/chat</a>. */
        PIN_MESSAGES(17);

        private final int value;
        private final int flag;

        Right(int value) {
            this.value = value;
            this.flag = 1 << value;
        }

        public int getValue() {
            return value;
        }

        public int getFlag() {
            return flag;
        }

        public static EnumSet<Right> of(ChatBannedRights data) {
            EnumSet<Right> set = EnumSet.noneOf(Right.class);
            int flags = data.flags();
            for (Right value : values()) {
                if ((flags & value.flag) != 0) {
                    set.add(value);
                }
            }
            return set;
        }
    }
}
