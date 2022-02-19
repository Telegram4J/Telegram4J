package telegram4j.core.util;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.*;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class EntityParserSupport {

    public static final Pattern USER_LINK_ID_PATTERN = Pattern.compile("^tg://user\\?id=(\\d{1,19})$", Pattern.CASE_INSENSITIVE);

    private EntityParserSupport() {}

    public static final Function<String, EntityParser> MARKDOWN_V2 = MarkdownV2EntityParser::new;
    public static final Function<String, EntityParser> HTML = HtmlEntityParser::new;

    public static Mono<Tuple2<String, List<MessageEntity>>> parse(MTProtoTelegramClient client, EntityParser parser) {
        List<EntityToken> tokens = new LinkedList<>();
        EntityToken t;
        while ((t = parser.nextToken()) != EntityToken.UNKNOWN) {
            tokens.add(t);
        }

        if (tokens.size() % 2 != 0) {
            throw new IllegalStateException("Incorrect token count (" + tokens.size() + ") is odd.");
        }

        String striped = parser.striped();

        // sort list and collect pairs from the nearest tokens
        tokens.sort(Comparator.comparing(EntityToken::type)
                .thenComparingInt(EntityToken::offset));

        return Flux.<MessageEntity>create(sink -> {
            for (var it = tokens.iterator(); it.hasNext(); ) {
                EntityToken begin = it.next();
                EntityToken end = it.next();

                int offset = begin.offset();
                int length = end.offset() - offset;

                switch (begin.type()) {
                    case MENTION:
                        sink.next(ImmutableMessageEntityMention.of(offset, length));
                        break;
                    case HASHTAG:
                        sink.next(ImmutableMessageEntityHashtag.of(offset, length));
                        break;
                    case BOT_COMMAND:
                        sink.next(ImmutableMessageEntityBotCommand.of(offset, length));
                        break;
                    case URL:
                        sink.next(ImmutableMessageEntityUrl.of(offset, length));
                        break;
                    case EMAIL_ADDRESS:
                        sink.next(ImmutableMessageEntityEmail.of(offset, length));
                        break;
                    case CASHTAG:
                        sink.next(ImmutableMessageEntityCashtag.of(offset, length));
                        break;
                    case PHONE_NUMBER:
                        sink.next(ImmutableMessageEntityPhone.of(offset, length));
                        break;
                    case UNDERLINE:
                        sink.next(ImmutableMessageEntityUnderline.of(offset, length));
                        break;
                    case BLOCK_QUOTE:
                        sink.next(ImmutableMessageEntityBlockquote.of(offset, length));
                        break;
                    case BANK_CARD_NUMBER:
                        sink.next(ImmutableMessageEntityBankCard.of(offset, length));
                        break;
                    case BOLD:
                        sink.next(ImmutableMessageEntityBold.of(offset, length));
                        break;
                    case ITALIC:
                        sink.next(ImmutableMessageEntityItalic.of(offset, length));
                        break;
                    case CODE:
                        sink.next(ImmutableMessageEntityCode.of(offset, length));
                        break;
                    case PRE:
                        sink.next(ImmutableMessageEntityPre.of(offset, length,
                                Objects.requireNonNullElse(begin.arg(), "")));
                        break;
                    case SPOILER:
                        sink.next(ImmutableMessageEntitySpoiler.of(offset, length));
                        break;
                    case STRIKETHROUGH:
                        sink.next(ImmutableMessageEntityStrike.of(offset, length));
                        break;
                    case MENTION_NAME:
                        String arg = Objects.requireNonNull(begin.arg());
                        long userId = Long.parseLong(arg);
                        client.getMtProtoResources()
                                .getStoreLayout()
                                .resolveUser(userId)
                                .map(p -> ImmutableInputMessageEntityMentionName.of(offset, length, p))
                                .subscribe(sink::next, sink::error, sink::complete);
                    break;
                    case TEXT_URL:
                        sink.next(ImmutableMessageEntityTextUrl.of(offset, length,
                                Objects.requireNonNullElse(begin.arg(), "")));
                        break;
                }
            }

            sink.complete();
        })
        .collectList()
        .map(list -> Tuples.of(striped, list));
    }
}
