package telegram4j.core.util.parser;

import telegram4j.core.object.MessageEntity;

import java.util.function.Function;

/** A functional interface for creating new instances of {@link EntityParser}. */
@FunctionalInterface
public interface EntityParserFactory extends Function<String, EntityParser> {

    /**
     * A factory which creates simple markdown markup parser.
     * Tokens with {@link MessageEntity.Type#MENTION_NAME} and {@link MessageEntity.Type#PRE}
     * types parse arguments using {@link String#trim()}
     *
     * <table class="striped">
     * <caption style="display:none">genConv</caption>
     * <thead>
     * <tr><th scope="col" style="vertical-align:bottom"> Format
     *     <th scope="col" style="vertical-align:bottom"> Token Type
     * </thead>
     * <tbody>
     * <tr><th scope="row" style="vertical-align:top"> {@code ~~text~~}
     *     <td> {@link MessageEntity.Type#STRIKETHROUGH}
     * <tr><th scope="row" style="vertical-align:top"> {@code **text**}
     *     <td> {@link MessageEntity.Type#BOLD}
     * <tr><th scope="row" style="vertical-align:top"> {@code _text_}
     *     <td> {@link MessageEntity.Type#ITALIC}
     * <tr><th scope="row" style="vertical-align:top"> {@code __text__}
     *     <td> {@link MessageEntity.Type#UNDERLINE}
     * <tr><th scope="row" style="vertical-align:top"> {@code [text](url)}
     *     <td> {@link MessageEntity.Type#TEXT_URL} or if url matches
     *     by {@link EntityParserSupport#USER_LINK_ID_PATTERN user link pattern}
     *     will be interpreted as {@link MessageEntity.Type#MENTION_NAME}
     * <tr><th scope="row" style="vertical-align:top"> {@code ||text||}
     *     <td> {@link MessageEntity.Type#SPOILER}
     * <tr><th scope="row" style="vertical-align:top"> {@code ```language\ntext```}
     *     <td> {@link MessageEntity.Type#PRE} with optional programming/markup language argument
     * <tr><th scope="row" style="vertical-align:top"> {@code `text`}
     *     <td> {@link MessageEntity.Type#CODE}
     * </tbody>
     * </table>
     */
    EntityParserFactory MARKDOWN_V2 = MarkdownV2EntityParser::new;

    /**
     * A factory which creates HTML-like text markup parser.
     * In case when html tag is {@code <a href="url">text</a>}
     * url argument can be optionally written
     * in single ({@code '\''}) or double ({@code '"'}) quotes.
     *
     * <table class="striped">
     * <caption style="display:none">genConv</caption>
     * <thead>
     * <tr><th scope="col" style="vertical-align:bottom"> HTML Tag
     *     <th scope="col" style="vertical-align:bottom"> Token Type
     * </thead>
     * <tbody>
     * <tr><th scope="row" style="vertical-align:top"> {@code <b>text</b>}
     *     <td> {@link MessageEntity.Type#BOLD}
     * <tr><th scope="row" style="vertical-align:top"> {@code <i>text</i>}
     *     <td> {@link MessageEntity.Type#ITALIC}
     * <tr><th scope="row" style="vertical-align:top"> {@code <u>text</u>}
     *     <td> {@link MessageEntity.Type#UNDERLINE}
     * <tr><th scope="row" style="vertical-align:top"> {@code <s>text</s>}
     *     <td> {@link MessageEntity.Type#STRIKETHROUGH}
     * <tr><th scope="row" style="vertical-align:top"> {@code <spoiler>text</spoiler>}
     *     <td> {@link MessageEntity.Type#SPOILER}
     * <tr><th scope="row" style="vertical-align:top"> {@code <code>text</code>}, {@code <c>text</c>}
     *     <td> {@link MessageEntity.Type#CODE}
     * <tr><th scope="row" style="vertical-align:top"> {@code <a href="url">text</a>}
     *     <td> {@link MessageEntity.Type#TEXT_URL} or if url matches
     *     by {@link EntityParserSupport#USER_LINK_ID_PATTERN user link pattern}
     *     will be interpreted as {@link MessageEntity.Type#MENTION_NAME}
     * <tr><th scope="row" style="vertical-align:top"> {@code <pre language="url">text</pre>}
     *     <td> {@link MessageEntity.Type#PRE} with optional programming/markup language
     * </tbody>
     * </table>
     */
    EntityParserFactory HTML = HtmlEntityParser::new;
}
