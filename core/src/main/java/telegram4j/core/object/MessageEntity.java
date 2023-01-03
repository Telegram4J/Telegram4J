package telegram4j.core.object;

import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.internal.MappingUtil;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.util.Id;
import telegram4j.core.util.parser.EntityParserSupport;
import telegram4j.tl.*;

import java.util.Objects;
import java.util.Optional;

/**
 * Representation of markup text entity.
 *
 * <p>For parsing entities use {@link EntityParserSupport} utility.
 */
public final class MessageEntity implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.MessageEntity data;
    private final String content;

    public MessageEntity(MTProtoTelegramClient client, telegram4j.tl.MessageEntity data, String text) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);

        this.content = text.substring(data.offset(), data.offset() + data.length());
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets type of the entity.
     *
     * @return The {@link Type} of the entity.
     */
    public Type getType() {
        return Type.of(data);
    }

    /**
     * Gets text substring of the entity.
     *
     * @return The text value of the entity.
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets offset index in the striped text (in UTF-8 codepoints).
     *
     * @return The offset index in the striped text.
     */
    public int getOffset() {
        return data.offset();
    }

    /**
     * Gets length of the entity in the text (in UTF-8 codepoints).
     * For computing end index of the entity use following code: {@code getOffset() + getLength()}
     *
     * @return The length of the entity in the text.
     */
    public int getLength() {
        return data.length();
    }

    /**
     * Gets programming language for code block, if {@code getType() == Type.PRE}.
     * Might be empty string if language isn't specified.
     *
     * @return The programming language name for code block.
     */
    public Optional<String> getLanguage() {
        return data.identifier() == MessageEntityPre.ID
                ? Optional.of((MessageEntityPre) data).map(MessageEntityPre::language)
                : Optional.empty();
    }

    /**
     * Gets url for <a href="https://www.google.com">text url</a>, if {@code getType() == Type.TEXT_URL}.
     *
     * @return The url for in-text url.
     */
    public Optional<String> getUrl() {
        return data.identifier() == MessageEntityTextUrl.ID
                ? Optional.of((MessageEntityTextUrl) data).map(MessageEntityTextUrl::url)
                : Optional.empty();
    }

    /**
     * Gets id of the mentioned user, if {@code getType() == Type.MENTION_NAME}.
     *
     * @return The id of the mentioned user.
     */
    public Optional<Id> getUserId() {
        return data.identifier() == MessageEntityMentionName.ID
                ? Optional.of((MessageEntityMentionName) data).map(e -> Id.ofUser(e.userId()))
                : Optional.empty();
    }

    /**
     * Requests to retrieve mentioned user.
     *
     * @return An {@link Mono} emitting on successful completion the {@link User mentioned user}.
     */
    public Mono<User> getUser() {
        return getUser(MappingUtil.IDENTITY_RETRIEVER);
    }

    /**
     * Requests to retrieve mentioned user using specified retrieval strategy.
     *
     * @param strategy The strategy to apply
     * @return An {@link Mono} emitting on successful completion the {@link User mentioned user}.
     */
    public Mono<User> getUser(EntityRetrievalStrategy strategy) {
        return Mono.justOrEmpty(getUserId())
                .flatMap(client.withRetrievalStrategy(strategy)::getUserById);
    }

    /**
     * Gets id of the custom emoji, if {@code getType() == Type.CUSTOM_EMOJI}.
     *
     * @return The id of custom emoji.
     */
    public Optional<Long> getDocumentId() {
        return data.identifier() == MessageEntityCustomEmoji.ID
                ? Optional.of(((MessageEntityCustomEmoji) data).documentId())
                : Optional.empty();
    }

    /**
     * Requests to retrieve custom emoji by document id.
     *
     * @return An {@link Mono} emitting on successful completion the {@link Sticker custom emoji}.
     */
    public Mono<Sticker> getCustomEmoji() {
        return Mono.justOrEmpty(getDocumentId())
                .flatMap(client::getCustomEmoji);
    }

    @Override
    public String toString() {
        return "MessageEntity{" +
                "data=" + data +
                ", content='" + content + '\'' +
                '}';
    }

    public enum Type {
        /** Message entity mentioning the channel or user through <i>@username</i>. */
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
         * Message entity representing a preformatted codeblock,
         * allowing the user to specify a programming language.
         */
        PRE,

        /** Message entity representing a text url in a format like this: <i>[text](url)</i>. */
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

        /** Message entity representing <a href="https://core.telegram.org/stickers#animated-emoji">custom emoji</a>. */
        CUSTOM_EMOJI;

        /**
         * Gets type of raw {@link telegram4j.tl.MessageEntity} data.
         *
         * @param data The raw data.
         * @return The {@code Type} of raw message entity data.
         */
        public static Type of(telegram4j.tl.MessageEntity data) {
            switch (data.identifier()) {
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
                case InputMessageEntityMentionName.ID: return MENTION_NAME; // for compatibility
                case MessageEntityPhone.ID: return PHONE_NUMBER;
                case MessageEntityCashtag.ID: return CASHTAG;
                case MessageEntityUnderline.ID: return UNDERLINE;
                case MessageEntityStrike.ID: return STRIKETHROUGH;
                case MessageEntityBlockquote.ID: return BLOCK_QUOTE;
                case MessageEntityBankCard.ID: return BANK_CARD_NUMBER;
                case MessageEntitySpoiler.ID: return SPOILER;
                case MessageEntityCustomEmoji.ID: return CUSTOM_EMOJI;
                // and MessageEntityUnknown
                default: throw new IllegalArgumentException("Unknown message type: " + data);
            }
        }
    }
}
