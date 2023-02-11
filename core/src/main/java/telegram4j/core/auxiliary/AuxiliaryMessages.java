package telegram4j.core.auxiliary;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.Message;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chat.PrivateChat;
import telegram4j.core.util.Id;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Container with found {@link Message}s with auxiliary {@link Chat} and {@link User} objects. */
public sealed class AuxiliaryMessages
        permits AuxiliaryChannelMessages, AuxiliaryMessagesSlice {

    private final MTProtoTelegramClient client;
    private final List<Message> messages;
    private final Map<Id, Chat> chats;
    private final Map<Id, User> users;

    public AuxiliaryMessages(MTProtoTelegramClient client, List<Message> messages,
                             Map<Id, Chat> chats, Map<Id, User> users) {
        this.client = client;
        this.messages = messages;
        this.chats = chats;
        this.users = users;
    }

    public MTProtoTelegramClient getClient() {
        return client;
    }

    /**
     * Gets immutable list of found {@link Message}s.
     *
     * @return The immutable {@link List} of found {@link Message}s.
     */
    public List<Message> getMessages() {
        return messages;
    }

    /**
     * Gets immutable map of {@link Chat}s mentioned in messages.
     * This map doesn't contain {@link PrivateChat} objects.
     *
     * @return The immutable {@link Map} of {@link Chat} mentioned in messages.
     */
    public Map<Id, Chat> getChats() {
        return chats;
    }

    public Optional<Chat> getChat(Id userId) {
        return Optional.ofNullable(chats.get(userId));
    }

    /**
     * Gets immutable map of {@link User}s mentioned in messages.
     *
     * @return The immutable {@link Map} of {@link User} mentioned in messages.
     */
    public Map<Id, User> getUsers() {
        return users;
    }

    public Optional<User> getUser(Id userId) {
        return Optional.ofNullable(users.get(userId));
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
