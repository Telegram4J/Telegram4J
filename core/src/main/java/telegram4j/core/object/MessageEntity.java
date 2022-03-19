package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.*;

import java.util.Objects;
import java.util.Optional;

public class MessageEntity implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.MessageEntity data;
    private final String content;

    public MessageEntity(MTProtoTelegramClient client, telegram4j.tl.MessageEntity data, String text) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
        this.content = text.substring(data.offset(), data.offset() + data.length());
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public Type getType() {
        return Type.of(data);
    }

    public String getContent() {
        return content;
    }

    public int getOffset() {
        return data.offset();
    }

    public int getLength() {
        return data.length();
    }

    public Optional<String> getLanguage() {
        return data.identifier() == MessageEntityPre.ID
                ? Optional.of((MessageEntityPre) data).map(MessageEntityPre::language)
                : Optional.empty();
    }

    public Optional<String> getUrl() {
        return data.identifier() == MessageEntityTextUrl.ID
                ? Optional.of((MessageEntityTextUrl) data).map(MessageEntityTextUrl::url)
                : Optional.empty();
    }

    public Optional<Id> getUserId() {
        // InputMessageEntityMentionName doesn't handle because it's an input entity, that maps into the MessageEntityMentionName
        return data.identifier() == MessageEntityMentionName.ID
                ? Optional.of((MessageEntityMentionName) data).map(e -> Id.ofUser(e.userId(), null))
                : Optional.empty();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageEntity that = (MessageEntity) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "MessageEntity{" +
                "data=" + data +
                ", content='" + content + '\'' +
                '}';
    }

    public enum Type {
        /** Message entity mentioning the current user. */
        MENTION,

        /** <i>#hashtag</i> message entity. */
        HASHTAG,

        /** Message entity representing a bot <i>/command</i>. */
        BOT_COMMAND,

        /** Message entity representing an in-text url: <i>https://google.com</i>. */
        URL,

        /** Message entity representing an <i>email@example.com</i>. */
        EMAIL_ADDRESS,

        /** Message entity representing <b>bold text</b>. */
        BOLD,

        /** Message entity representing <i>italic text</i>. */
        ITALIC,

        /** Message entity representing a {@code codeblock}. */
        CODE,

        /**
         * Message entity representing a preformatted {@code codeblock},
         * allowing the user to specify a programming language for the codeblock.
         */
        PRE,

        /** Message entity representing a text url: <i>[text](url)</i>. */
        TEXT_URL,

        /** Message entity representing a <a href="https://t.me/test">user mention</a>. */
        MENTION_NAME,

        /** Message entity representing a <b>$cashtag</b>. */
        CASHTAG,

        /** Message entity representing a phone number. */
        PHONE_NUMBER,

        /** Message entity representing <u>underlined text</u>. */
        UNDERLINE,

        /** Message entity representing <s>strikethrough text</s>. */
        STRIKETHROUGH,

        /** Message entity representing a block quote. */
        BLOCK_QUOTE,

        /** Indicates a credit card number. */
        BANK_CARD_NUMBER,

        /** Message entity representing a spoiler text. */
        SPOILER,

        /** Unknown message entity. */
        UNKNOWN;

        static Type of(telegram4j.tl.MessageEntity data) {
            switch (data.identifier()) {
                case MessageEntityUnknown.ID: return UNKNOWN;
                case MessageEntityMention.ID: return MENTION;
                case MessageEntityHashtag.ID: return HASHTAG;
                case MessageEntityBotCommand.ID: return BOT_COMMAND;
                case MessageEntityUrl.ID: return URL;
                case MessageEntityEmail.ID: return EMAIL_ADDRESS;
                case MessageEntityBold.ID: return BOLD;
                case MessageEntityItalic.ID: return ITALIC;
                case MessageEntityCode.ID: return CODE;
                case MessageEntityPre.ID: return PRE;
                case MessageEntityTextUrl.ID: return TEXT_URL;
                case MessageEntityMentionName.ID:
                case InputMessageEntityMentionName.ID: return MENTION_NAME;
                case MessageEntityPhone.ID: return PHONE_NUMBER;
                case MessageEntityCashtag.ID: return CASHTAG;
                case MessageEntityUnderline.ID: return UNDERLINE;
                case MessageEntityStrike.ID: return STRIKETHROUGH;
                case MessageEntityBlockquote.ID: return BLOCK_QUOTE;
                case MessageEntityBankCard.ID: return BANK_CARD_NUMBER;
                case MessageEntitySpoiler.ID: return SPOILER;
                default: throw new IllegalArgumentException("Unknown message type: " + data);
            }
        }
    }
}
