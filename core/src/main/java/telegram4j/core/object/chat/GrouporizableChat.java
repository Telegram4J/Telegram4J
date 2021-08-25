package telegram4j.core.object.chat;

import java.util.Optional;

public interface GrouporizableChat extends Chat {

    Optional<String> getTitle();

    Optional<String> getDescription();

    Optional<String> getInviteLink();
}
