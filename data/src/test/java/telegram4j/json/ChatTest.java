package telegram4j.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatTest extends DeserializationTest {

    public ChatTest() {
        super(ChatData.class);
    }

    @Test
    void chat() {
        ChatData expected = ChatData.builder()
                .id(-567859824)
                .type(ChatType.GROUP)
                .allMembersAreAdministrators(false)
                .title("MindustryInside")
                .username("mindustry-inside")
                .firstName("Mindustry")
                .lastName("Inside")
                .bio(":eyes:")
                .description("Group description")
                .build();

        ChatData actual = read("/json/Chat.json");
        assertEquals(expected, actual);
    }
}
