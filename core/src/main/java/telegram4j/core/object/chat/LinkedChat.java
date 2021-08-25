package telegram4j.core.object.chat;

import telegram4j.core.object.Id;

import java.util.Optional;

public interface LinkedChat extends GrouporizableChat {

    Optional<Id> getLinkedChatId();
}
