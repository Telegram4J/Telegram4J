package telegram4j.core.dispatch;

import reactor.core.publisher.Mono;
import telegram4j.core.TelegramClient;
import telegram4j.core.event.ChatMemberUpdateEvent;
import telegram4j.core.event.SelfChatMemberUpdateEvent;
import telegram4j.core.object.ChatInviteLink;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.object.chatmember.ChatMember;
import telegram4j.core.util.EntityUtil;
import telegram4j.json.ChatMemberUpdatedData;

class ChatMemberDispatcherHandlers {

    static class SelfChatMemberUpdate implements DispatchHandler<SelfChatMemberUpdateEvent, Void> {

        @Override
        public boolean canHandle(UpdateContext<Void> update) {
            return update.getUpdateData().myChatMember().isPresent();
        }

        @Override
        public Mono<SelfChatMemberUpdateEvent> handle(UpdateContext<Void> update) {
            TelegramClient client = update.getClient();

            ChatMemberUpdatedData data = update.getUpdateData().myChatMember()
                    .orElseThrow(IllegalStateException::new);

            Chat chat = EntityUtil.getChat(client, data.chat());
            User fromUser = new User(client, data.fromUser());
            ChatMember old = EntityUtil.getChatMember(client, data.oldChatMember());
            ChatMember current = EntityUtil.getChatMember(client, data.newChatMember());
            ChatInviteLink chatInviteLink = data.inviteLink()
                    .map(chatInviteLinkData1 -> new ChatInviteLink(client, chatInviteLinkData1))
                    .orElse(null);

            return Mono.just(new SelfChatMemberUpdateEvent(client, chat, fromUser, data.date(), old, current, chatInviteLink));
        }
    }

    static class ChatMemberUpdate implements DispatchHandler<ChatMemberUpdateEvent, Void> {

        @Override
        public boolean canHandle(UpdateContext<Void> update) {
            return update.getUpdateData().chatMember().isPresent();
        }

        @Override
        public Mono<ChatMemberUpdateEvent> handle(UpdateContext<Void> update) {
            TelegramClient client = update.getClient();

            ChatMemberUpdatedData data = update.getUpdateData().chatMember()
                    .orElseThrow(IllegalStateException::new);

            Chat chat = EntityUtil.getChat(client, data.chat());
            User fromUser = new User(client, data.fromUser());
            ChatMember old = EntityUtil.getChatMember(client, data.oldChatMember());
            ChatMember current = EntityUtil.getChatMember(client, data.newChatMember());
            ChatInviteLink chatInviteLink = data.inviteLink()
                    .map(chatInviteLinkData1 -> new ChatInviteLink(client, chatInviteLinkData1))
                    .orElse(null);

            return Mono.just(new ChatMemberUpdateEvent(client, chat, fromUser, data.date(), old, current, chatInviteLink));
        }
    }
}
