package telegram4j.core.util.parser;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.MessageEntity.Type;
import telegram4j.tl.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static reactor.util.function.Tuples.of;

/** Markup parsing utilities. */
public final class EntityParserSupport {

    public static final Pattern USER_LINK_ID_PATTERN = Pattern.compile("^tg://user\\?id=(\\d{1,19})$", Pattern.CASE_INSENSITIVE);
    // Patterns are taken from TDLib, https://github.com/tdlib/td/blob/master/td/telegram/MessageEntity.cpp
    public static final Pattern EMAIL_PATTERN = Pattern.compile(
            "([\\d\\w-]{0,26}[.+]){0,10}[\\d\\w-]{1,35}@(([a-z\\d][\\d\\w-]{0,28})?[a-z\\d][.]){1,6}[a-z]{2,8}",
            Pattern.CASE_INSENSITIVE);
    public static final Pattern HASHTAG_PATTERN = Pattern.compile(
            "(?<=^|[^\\d_\\pL\\x{200c}])#([\\d_\\pL\\x{200c}]{1,256})(?![\\d_\\pL\\x{200c}]*#)",
            Pattern.UNICODE_CASE);
    public static final Pattern BOT_COMMAND_PATTERN = Pattern.compile(
            "(?<!\\b|[/<>])/(\\w{1,64})(?:@(\\w{3,32}))?(?!\\B|[/<>])",
            Pattern.UNICODE_CASE);
    public static final Pattern CASHTAG_PATTERN = Pattern.compile(
            "(?<=^|[^$\\d_\\pL\\x{200c}])\\$(1INCH|[A-Z]{1,8})(?![$\\d_\\pL\\x{200c}])",
            Pattern.UNICODE_CASE);
    public static final Pattern BANK_CARD_PATTERN = Pattern.compile("(?<=^|[^+_\\pL\\d-.,])[\\d -]{13,}([^_\\pL\\d-]|$)");

    private static final List<Tuple2<Pattern, Type>> PATTERNS =
            List.of(of(EMAIL_PATTERN, Type.EMAIL_ADDRESS),
                    of(HASHTAG_PATTERN, Type.HASHTAG),
                    of(BOT_COMMAND_PATTERN, Type.BOT_COMMAND),
                    of(CASHTAG_PATTERN, Type.CASHTAG),
                    of(BANK_CARD_PATTERN, Type.BANK_CARD_NUMBER));

    private EntityParserSupport() {
    }

    /**
     * Collects and sorts tokens from specified parser to the nearest pairs.
     * After sorting tokens converts to the {@link MessageEntity} and collects to the result list.
     *
     * @param client The client to resolve mentions.
     * @param parser The message entity parser.
     * @return A {@link Mono} emitting on successful completion {@link Tuple2} with striped text and list of parsed message entities.
     */
    public static Mono<Tuple2<String, List<MessageEntity>>> parse(MTProtoTelegramClient client, EntityParser parser) {
        List<EntityToken> tokens = new ArrayList<>();
        EntityToken t;
        while ((t = parser.nextToken()) != null) {
            tokens.add(t);
        }

        if (tokens.size() % 2 != 0) {
            return Mono.error(new IllegalStateException("Incorrect token count (" + tokens.size() + ") is odd."));
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
                        String arg = Objects.requireNonNull(begin.arg(), () ->
                                "Absent userId value for token begin: " + begin);
                        long userId = Long.parseLong(arg);
                        client.getMtProtoResources().getStoreLayout()
                                .resolveUser(userId)
                                .map(p -> ImmutableInputMessageEntityMentionName.of(offset, length, p))
                                .subscribe(sink::next, sink::error);
                        break;
                    case CUSTOM_EMOJI:
                        // TODO
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
        .doOnNext(list -> list.addAll(scanUniform(striped)))
        .map(list -> of(striped, list));
    }

    public static List<MessageEntity> scanUniform(String text) {
        List<MessageEntity> list = new ArrayList<>();

        for (var t : PATTERNS) {
            Matcher m = t.getT1().matcher(text);
            if (!m.find()) {
                continue;
            }

            int offset = m.start();
            int length = m.end() - offset;

            switch (t.getT2()) {
                case EMAIL_ADDRESS:
                    list.add(ImmutableMessageEntityEmail.of(offset, length));
                    break;
                case HASHTAG:
                    list.add(ImmutableMessageEntityHashtag.of(offset, length));
                    break;
                case BOT_COMMAND:
                    list.add(ImmutableMessageEntityBotCommand.of(offset, length));
                    break;
                case CASHTAG:
                    list.add(ImmutableMessageEntityCashtag.of(offset, length));
                    break;
                case BANK_CARD_NUMBER:
                    list.add(ImmutableMessageEntityBankCard.of(offset, length));
                    break;
                default:
                    throw new IllegalStateException("Incorrect entity type: " + t);
            }
        }

        return list;
    }
}
