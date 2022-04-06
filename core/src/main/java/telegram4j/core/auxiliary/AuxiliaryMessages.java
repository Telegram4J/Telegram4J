package telegram4j.core.auxiliary;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;

import java.util.List;

/** Container with found {@link Message}s with auxiliary {@link Chat} and {@link User} objects. */
public class AuxiliaryMessages {
    private final MTProtoTelegramClient client;
    private final List<Message> messages;
    private final List<Chat> chats;
    private final List<User> users;

    public AuxiliaryMessages(MTProtoTelegramClient client, List<Message> messages,
                             List<Chat> chats, List<User> users) {
        this.client = client;
        this.messages = messages;
        this.chats = chats;
        this.users = users;
    }

    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets list of found {@link Message}s.
     *
     * @return The {@link List} of found {@link Message}s.
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Gets list of {@link Chat}s mentioned in messages.
     *
     * @return The {@link List} of {@link Chat} mentioned in messages.
     */
    public List<Chat> getChats() {
        return chats;
    }

    /**
     * Gets list of {@link User}s mentioned in messages.
     *
     * @return The {@link List} of {@link User} mentioned in messages.
     */
    public List<User> getUsers() {
        return users;
    }

    @Override
    public String toString() {
        return "AuxiliaryMessages{" +
                "messages=" + messages +
                ", chats=" + chats +
                ", users=" + users +
                '}';
    }
}
