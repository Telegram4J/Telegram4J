package telegram4j.core.util;

import java.util.function.Function;

@FunctionalInterface
public interface EntityParserFactory extends Function<String, EntityParser> {

    EntityParserFactory MARKDOWN_V2 = MarkdownV2EntityParser::new;

    EntityParserFactory HTML = HtmlEntityParser::new;
}
