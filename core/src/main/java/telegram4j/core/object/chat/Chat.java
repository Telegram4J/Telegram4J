package telegram4j.core.object.chat;

import telegram4j.core.object.ChatPhoto;
import telegram4j.json.api.Id;
import telegram4j.core.object.Message;
import telegram4j.core.object.TelegramObject;
import telegram4j.json.ChatData;
import telegram4j.json.ChatType;

import java.util.Optional;

public interface Chat extends TelegramObject {

    Id getId();

    ChatType getType();

    ChatData getData();

    Optional<ChatPhoto> getPhoto();

    Optional<Message> getPinnedMessage();

    Optional<Integer> getMessageAutoDeleteTime();
}
