package telegram4j.core.object.chat;

import telegram4j.core.object.ChatPermissions;

import java.util.Optional;

public interface GrouporizableChat extends Chat {

    Optional<String> getTitle();

    Optional<String> getDescription();

    Optional<String> getInviteLink();

    Optional<ChatPermissions> getPermissions();
}
