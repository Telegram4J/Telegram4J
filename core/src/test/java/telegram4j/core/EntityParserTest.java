package telegram4j.core;

import org.junit.jupiter.api.Test;
import reactor.util.function.Tuple2;
import telegram4j.core.util.EntityParser;
import telegram4j.tl.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntityParserTest {

    @Test
    void all() {
        List<MessageEntity> exc = Arrays.asList(
                ImmutableMessageEntityBold.of(1, 7),
                ImmutableMessageEntityStrike.of(13, 3),
                ImmutableMessageEntityMentionName.of(19, 24, 123456789),
                ImmutableMessageEntityItalic.of(44, 1),
                ImmutableMessageEntityUnderline.of(46, 2),
                ImmutableMessageEntityCode.of(50, 9),
                ImmutableMessageEntityTextUrl.of(65, 1, "https://google.com"),
                ImmutableMessageEntityPre.of(67, 4, ""));

        String s = "\\**Message*char ~123~ № [inline mention of a user](tg://user?id=123456789) _a_ __bc__  `code code` text " +
                "[g](https://google.com) ```\nowo\n```";

        Tuple2<String, List<MessageEntity>> tuple = EntityParser.parse(s, EntityParser.Mode.MARKDOWN_V2);

        assertEquals(exc, tuple.getT2());
    }
}
