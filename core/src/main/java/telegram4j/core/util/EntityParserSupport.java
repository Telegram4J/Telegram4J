package telegram4j.core.util;

import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.tl.*;

import java.util.*;
import java.util.function.Function;

public final class EntityParserSupport {

    private EntityParserSupport() {}

    public static final Function<String, EntityParser> MARKDOWN_V2 = MarkdownV2EntityParser::new;

    public static Tuple2<String, List<MessageEntity>> parse(EntityParser parser) {
        List<EntityToken> tokens = new LinkedList<>();
        EntityToken t;
        while ((t = parser.nextToken()) != EntityToken.UNKNOWN) {
            tokens.add(t);
        }

        if (tokens.size() % 2 != 0) {
            throw new IllegalStateException("Incorrect token count (" + tokens.size() + ") is odd.");
        }

        // sort list and collect pairs from the nearest tokens
        tokens.sort(Comparator.comparing(EntityToken::type)
                .thenComparingInt(EntityToken::offset));

        List<telegram4j.tl.MessageEntity> entities = new ArrayList<>(tokens.size() / 2);
        for (var it = tokens.iterator(); it.hasNext(); ) {
            EntityToken begin = it.next();
            EntityToken end = it.next();

            int offset = begin.offset();
            int length = end.offset() - offset;

            switch (begin.type()) {
                case MENTION:
                    entities.add(ImmutableMessageEntityMention.of(offset, length));
                    break;
                case HASHTAG:
                    entities.add(ImmutableMessageEntityHashtag.of(offset, length));
                    break;
                case BOT_COMMAND:
                    entities.add(ImmutableMessageEntityBotCommand.of(offset, length));
                    break;
                case URL:
                    entities.add(ImmutableMessageEntityUrl.of(offset, length));
                    break;
                case EMAIL_ADDRESS:
                    entities.add(ImmutableMessageEntityEmail.of(offset, length));
                    break;
                case CASHTAG:
                    entities.add(ImmutableMessageEntityCashtag.of(offset, length));
                    break;
                case PHONE_NUMBER:
                    entities.add(ImmutableMessageEntityPhone.of(offset, length));
                    break;
                case UNDERLINE:
                    entities.add(ImmutableMessageEntityUnderline.of(offset, length));
                    break;
                case BLOCK_QUOTE:
                    entities.add(ImmutableMessageEntityBlockquote.of(offset, length));
                    break;
                case BANK_CARD_NUMBER:
                    entities.add(ImmutableMessageEntityBankCard.of(offset, length));
                    break;
                case BOLD:
                    entities.add(ImmutableMessageEntityBold.of(offset, length));
                    break;
                case ITALIC:
                    entities.add(ImmutableMessageEntityItalic.of(offset, length));
                    break;
                case CODE:
                    entities.add(ImmutableMessageEntityCode.of(offset, length));
                    break;
                case PRE:
                    entities.add(ImmutableMessageEntityPre.of(offset, length,
                            Objects.requireNonNullElse(begin.arg(), "")));
                    break;
                case SPOILER:
                    entities.add(ImmutableMessageEntitySpoiler.of(offset, length));
                    break;
                case STRIKETHROUGH:
                    entities.add(ImmutableMessageEntityStrike.of(offset, length));
                    break;
                case MENTION_NAME:
                    String arg = Objects.requireNonNull(begin.arg());
                    entities.add(ImmutableMessageEntityMentionName.of(offset, length, Long.parseLong(arg)));
                    break;
                case TEXT_URL:
                    entities.add(ImmutableMessageEntityTextUrl.of(offset, length,
                            Objects.requireNonNullElse(begin.arg(), "")));
                    break;
            }
        }

        return Tuples.of(parser.striped(), entities);
    }
}
