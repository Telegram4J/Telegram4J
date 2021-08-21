package telegram4j.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageTest extends DeserializationTest {

    public MessageTest() {
        super(MessageData.class);
    }

    @Test
    void message() {
        MessageData expected = MessageData.builder()
                .messageId(14)
                .fromUser(UserData.builder()
                        .id(628911901)
                        .isBot(false)
                        .firstName(":earth_africa: Skat")
                        .username("Skat_ina")
                        .languageCode("ru")
                        .build())
                .chat(ChatData.builder()
                        .id(-567859824)
                        .title("MindustrInside")
                        .type(ChatType.GROUP)
                        .allMembersAreAdministrators(false)
                        .build())
                .date(1629498851)
                .migrateToChatId(-1001538393672L)
                .build();

        MessageData actual = readJson("/json/Message.json");
        assertEquals(expected, actual);
    }
}
