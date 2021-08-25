package telegram4j.core.object.chat;

import java.util.Optional;

public interface NamedChat extends Chat {

    Optional<String> getUsername();
}
