package telegram4j.core;

import org.junit.jupiter.api.Test;
import telegram4j.core.util.EntityParserSupport;
import telegram4j.tl.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityParserTest {

    @Test
    void markdownV2() {
        var exc = List.of(
                ImmutableMessageEntityBold.of(0, 12),
                ImmutableMessageEntityItalic.of(51, 11),
                ImmutableMessageEntityCode.of(78, 11),
                ImmutableMessageEntityPre.of(94, 23, "java"),
                ImmutableMessageEntityTextUrl.of(90, 3, "https://google.com"),
                ImmutableMessageEntityMentionName.of(26, 24, 123456789),
                ImmutableMessageEntityUnderline.of(63, 14),
                ImmutableMessageEntityStrike.of(14, 11));

        String original = "**bold text\\*\\*** ~~strike text~~ [inline mention of a user](tg://user?id=123456789) " +
                "_italic text_ __underline text__ `inline code` " +
                "[url](https://google.com) ```java\n" +
                "public class Clazz {}" +
                "\n```";

        String striped = "bold text\\*\\* strike text inline mention of a user italic text underline text inline code url \n" +
                "public class Clazz {}\n";

        var tuple = EntityParserSupport.parse(
                EntityParserSupport.MARKDOWN_V2.apply(original));

        assertEquals(striped, tuple.getT1());
        assertEquals(exc, tuple.getT2());
    }
}
