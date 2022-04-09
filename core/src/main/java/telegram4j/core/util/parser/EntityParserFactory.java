package telegram4j.core.util.parser;

import java.util.function.Function;

/** A functional interface for creating new instances of {@link EntityParser}. */
@FunctionalInterface
public interface EntityParserFactory extends Function<String, EntityParser> {

    /** A factory which creates default markdown v2 entity parser. */
    EntityParserFactory MARKDOWN_V2 = MarkdownV2EntityParser::new;

    /** A factory which creates default html entity parser. */
    EntityParserFactory HTML = HtmlEntityParser::new;
}
