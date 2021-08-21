package telegram4j.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ChatMemberUpdatedTest extends DeserializationTest {

    public ChatMemberUpdatedTest() {
        super(ChatMemberUpdatedData.class);
    }

    @Test
    void chatMemberUpdated() {
        ChatMemberUpdatedData expected = ChatMemberUpdatedData.builder()
                .chat(ChatData.builder()
                        .id(-1001538393672L)
                        .title("MindustrInside")
                        .type(ChatType.SUPERGROUP)
                        .build())
                .fromUser(UserData.builder()
                        .id(628911901)
                        .isBot(false)
                        .firstName(":earth_africa: Skat")
                        .username("Skat_ina")
                        .languageCode("ru")
                        .build())
                .date(1629498880)
                .oldChatMember(ChatMemberData.ChatMemberMemberData.builder()
                        .user(UserData.builder()
                                .id(1995173476L)
                                .isBot(true)
                                .firstName("Inside")
                                .username("SkatTestBot")
                                .build())
                        .build())
                .newChatMember(ChatMemberData.ChatMemberAdministratorData.builder()
                        .user(UserData.builder()
                                .id(1995173476L)
                                .isBot(true)
                                .firstName("Inside")
                                .username("SkatTestBot")
                                .build())
                        .canBeEdited(false)
                        .canManageChat(true)
                        .canChangeInfo(true)
                        .canDeleteMessages(true)
                        .canInviteUsers(true)
                        .canRestrictMembers(true)
                        .canPinMessages(true)
                        .canPromoteMembers(false)
                        .canManageVoiceChats(true)
                        .isAnonymous(false)
                        .build())
                .build();

        ChatMemberUpdatedData actual = read("/json/ChatMemberUpdated.json");
        assertEquals(expected, actual);
    }
}
