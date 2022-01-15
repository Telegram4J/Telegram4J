package telegram4j.core.object.chat;

import java.util.Optional;

public interface Channel extends Chat {

    String getTitle();

    Optional<String> getUsername();

    Optional<String> getAbout();
}
