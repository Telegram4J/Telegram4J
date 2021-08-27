package telegram4j.json;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateTest extends DeserializationTest {

    public UpdateTest() {
        super(UpdateData.class);
    }

    @Test
    void update() {
        List<UpdateData> expected = new ArrayList<>(Collections.emptyList());
        expected.add(UpdateData.builder()
                .updateId(264612701)
                .myChatMember(ChatMemberUpdatedData.builder()
                        .chat(ChatData.builder()
                                .id(-1001538393672L)
                                .title("MindustryInside")
                                .type(ChatType.SUPERGROUP)
                                .build())
                        .fromUser(UserData.builder()
                                .id(628911901)
                                .isBot(false)
                                .firstName(":earth_africa: Skat")
                                .username("Skat_ina")
                                .languageCode("ru")
                                .build())
                        .date(1629498851)
                        .oldChatMember(ChatMemberData.builder()
                                .user(UserData.builder()
                                        .id(1995173476L)
                                        .isBot(true)
                                        .firstName("Inside")
                                        .username("SkatTestBot")
                                        .build())
                                .status(ChatMemberStatus.LEFT)
                                .build())
                        .newChatMember(ChatMemberData.builder()
                                .user(UserData.builder()
                                        .id(1995173476L)
                                        .isBot(true)
                                        .firstName("Inside")
                                        .username("SkatTestBot")
                                        .build())
                                .status(ChatMemberStatus.MEMBER)
                                .build())

                        .build())
                .build());

        expected.add(UpdateData.builder()
                .updateId(264612702)
                .message(MessageData.builder()
                        .messageId(14L)
                        .fromUser(UserData.builder()
                                .id(628911901)
                                .isBot(false)
                                .firstName(":earth_africa: Skat")
                                .username("Skat_ina")
                                .languageCode("ru")
                                .build())
                        .chat(ChatData.builder()
                                .id(-567859824L)
                                .title("MindustryInside")
                                .type(ChatType.GROUP)
                                .allMembersAreAdministrators(false)
                                .build())
                        .date(1629498851)
                        .migrateToChatId(-1001538393672L)
                        .build())
                .build());

        expected.add(UpdateData.builder()
                .updateId(264612703)
                .message(MessageData.builder()
                        .messageId(1L)
                        .fromUser(UserData.builder()
                                .id(1087968824L)
                                .isBot(true)
                                .firstName("Group")
                                .username("GroupAnonymousBot")
                                .build())
                        .senderChat(ChatData.builder()
                                .id(-1001538393672L)
                                .title("MindustryInside")
                                .type(ChatType.SUPERGROUP)
                                .build())
                        .chat(ChatData.builder()
                                .id(-1001538393672L)
                                .title("MindustryInside")
                                .type(ChatType.SUPERGROUP)
                                .build())
                        .date(1629498851)
                        .migrateFromChatId(-567859824L)
                        .build())
                .build());

        expected.add(UpdateData.builder()
                .updateId(264612704)
                .myChatMember(ChatMemberUpdatedData.builder()
                        .chat(ChatData.builder()
                                .id(-1001538393672L)
                                .title("MindustryInside")
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
                        .oldChatMember(ChatMemberData.builder()
                                .user(UserData.builder()
                                        .id(1995173476L)
                                        .isBot(true)
                                        .firstName("Inside")
                                        .username("SkatTestBot")
                                        .build())
                                .status(ChatMemberStatus.MEMBER)
                                .build())
                        .newChatMember(ChatMemberData.builder()
                                .user(UserData.builder()
                                        .id(1995173476L)
                                        .isBot(true)
                                        .firstName("Inside")
                                        .username("SkatTestBot")
                                        .build())
                                .status(ChatMemberStatus.ADMINISTRATOR)
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
                        .build())
                .build());

        List<UpdateData> actual = readJsonList("/json/Update.json");
        assertEquals(expected, actual);
    }
}
