package telegram4j.core.event;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.core.object.ChatInviteLink;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chatmember.ChatMember;

import java.time.Instant;
import java.util.Optional;

public class SelfChatMemberUpdateEvent extends Event {

    private final Chat chat;
    private final User from;
    private final int date;
    private final ChatMember oldChatMember;
    private final ChatMember newChatMember;
    @Nullable
    private final ChatInviteLink chatInviteLink;

    public SelfChatMemberUpdateEvent(TelegramClient client, Chat chat, User from, int date,
                                     ChatMember oldChatMember, ChatMember newChatMember,
                                     @Nullable ChatInviteLink chatInviteLink) {
        super(client);
        this.chat = chat;
        this.from = from;
        this.date = date;
        this.oldChatMember = oldChatMember;
        this.newChatMember = newChatMember;
        this.chatInviteLink = chatInviteLink;
    }

    public Chat getChat() {
        return chat;
    }

    public User getFrom() {
        return from;
    }

    public Instant getTimestamp() {
        return Instant.ofEpochSecond(date);
    }

    public ChatMember getOldChatMember() {
        return oldChatMember;
    }

    public ChatMember getNewChatMember() {
        return newChatMember;
    }

    public Optional<ChatInviteLink> getChatInviteLink() {
        return Optional.ofNullable(chatInviteLink);
    }

    @Override
    public String toString() {
        return "SelfChatMemberUpdateEvent{" +
                "chat=" + chat +
                ", from=" + from +
                ", date=" + date +
                ", oldChatMember=" + oldChatMember +
                ", newChatMember=" + newChatMember +
                ", chatInviteLink=" + chatInviteLink +
                "} " + super.toString();
    }
}
