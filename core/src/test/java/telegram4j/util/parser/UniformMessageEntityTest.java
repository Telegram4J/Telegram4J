package telegram4j.util.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import telegram4j.tl.ImmutableMessageEntityBotCommand;
import telegram4j.tl.ImmutableMessageEntityEmail;
import telegram4j.tl.ImmutableMessageEntityHashtag;
import telegram4j.tl.MessageEntity;

import java.util.List;

import static telegram4j.core.util.parser.EntityParserSupport.scanUniform;

class UniformMessageEntityTest {

    @Test
    void test() {

        eq("", List.of());
        eq("send `/help@botbot` to get help!", List.of(ImmutableMessageEntityBotCommand.of(6, 12)));
        eq("example@domain.com says #sus :flushed:", List.of(
                ImmutableMessageEntityEmail.of(0, 18),
                ImmutableMessageEntityHashtag.of(24, 4)));
        eq("#sus", List.of(ImmutableMessageEntityHashtag.of(0, 4)));
    }

    static void eq(String text, List<MessageEntity> expected) {
        var list = scanUniform(text);
        Assertions.assertIterableEquals(expected, list);
    }
}
