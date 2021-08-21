package telegram4j.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserTest extends DeserializationTest {

    public UserTest() {
        super(UserData.class);
    }

    @Test
    void user() {
        UserData expected = UserData.builder()
                .id(1022839465)
                .isBot(false)
                .firstName("Ivan")
                .lastName("Ivanov")
                .username("ivanivanov1337")
                .languageCode("ru")
                .canJoinGroups(false)
                .canReadAllGroupMessages(true)
                .supportsInlineQueries(true)
                .build();

        UserData actual = read("/json/User.json");
        assertEquals(expected, actual);
    }
}
