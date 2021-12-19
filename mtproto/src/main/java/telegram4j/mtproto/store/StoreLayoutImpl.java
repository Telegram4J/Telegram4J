package telegram4j.mtproto.store;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.auth.AuthorizationKeyHolder;
import telegram4j.tl.*;
import telegram4j.tl.updates.State;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static telegram4j.mtproto.util.TlEntityUtil.peerId;

public class StoreLayoutImpl implements StoreLayout {

    private final ConcurrentMap<MessageId, Message> messages = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Chat> chats = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, BaseUser> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, InputPeer> usernames = new ConcurrentHashMap<>();
    private final ConcurrentMap<DataCenter, AuthorizationKeyHolder> authorizationKeys = new ConcurrentHashMap<>();

    private volatile long selfId;
    private volatile State state;

    @Override
    public Mono<State> getCurrentState() {
        return Mono.justOrEmpty(state);
    }

    @Override
    public Mono<Long> getSelfId() {
        return Mono.justOrEmpty(selfId);
    }

    @Override
    public Mono<InputPeer> resolvePeer(String username) {
        return Mono.fromSupplier(() -> usernames.get(stripUsername(username)));
    }

    @Override
    public Mono<Message> getMessageById(long chatId, int messageId) {
        return Mono.fromSupplier(() -> messages.get(new MessageId(messageId, chatId)));
    }

    @Override
    public Mono<Chat> getChatById(long chatId) {
        return Mono.fromSupplier(() -> chats.get(chatId));
    }

    @Override
    public Mono<User> getUserById(long userId) {
        return Mono.fromSupplier(() -> users.get(userId));
    }

    @Override
    public Mono<AuthorizationKeyHolder> getAuthorizationKey(DataCenter dc) {
        return Mono.fromSupplier(() -> authorizationKeys.get(dc));
    }

    @Override
    public Mono<Void> updateSelfId(long userId) {
        return Mono.fromRunnable(() -> selfId = userId);
    }

    @Override
    public Mono<Void> updateState(State state) {
        return Mono.fromRunnable(() -> this.state = state);
    }

    @Override
    public Mono<Void> updateAuthorizationKey(AuthorizationKeyHolder authorizationKey) {
        return Mono.fromRunnable(() -> authorizationKeys.put(authorizationKey.getDc(), authorizationKey));
    }

    @Override
    public Mono<Void> onNewMessage(Message message, List<Chat> chats, List<User> users) {
        return Mono.fromRunnable(() -> {
            messages.put(create(message), message);
            for (Chat chat : chats) {
                this.chats.put(chat.id(), chat);
            }

            users.forEach(this::saveUser);
        });
    }

    @Override
    public Mono<Message> onEditMessage(Message message, List<Chat> chats, List<User> users) {
        return Mono.fromSupplier(() -> {
            Message old = messages.put(create(message), message);
            for (Chat chat : chats) {
                this.chats.put(chat.id(), chat);
            }

            users.forEach(this::saveUser);
            return old;
        });
    }

    @Override
    public Mono<Void> onChannelUserTyping(UpdateChannelUserTyping action, List<Chat> chats, List<User> users) {
        return Mono.fromRunnable(() -> {
            for (Chat chat : chats) {
                this.chats.put(chat.id(), chat);
            }

            users.forEach(this::saveUser);
        });
    }

    @Override
    public Mono<Void> onChatUserTyping(UpdateChatUserTyping action, List<Chat> chats, List<User> users) {
        return Mono.fromRunnable(() -> {
            for (Chat chat : chats) {
                this.chats.put(chat.id(), chat);
            }

            users.forEach(this::saveUser);
        });
    }

    @Override
    public Mono<UserNameFields> onUserNameUpdate(UpdateUserName action, List<Chat> chats, List<User> users) {
        return Mono.fromSupplier(() -> {
            BaseUser u = this.users.get(action.userId());
            UserNameFields f = new UserNameFields(u.username(), u.firstName(), u.lastName());
            this.users.computeIfPresent(action.userId(), (k, v) -> ImmutableBaseUser.copyOf(u).withUsername(u.username()).withFirstName(u.firstName()).withLastName(u.lastName()));
            for (Chat chat : chats) {
                this.chats.put(chat.id(), chat);
            }

            users.forEach(this::saveUser);

            return f;
        });
    }

    @Override
    public Mono<Void> onUserPhoneUpdate(UpdateUserPhone action, List<Chat> chats, List<User> users) {
        return Mono.fromRunnable(() -> {
            for (Chat chat : chats) {
                this.chats.put(chat.id(), chat);
            }

            users.forEach(this::saveUser);
        });
    }

    @Override
    public Mono<Void> onUserPhotoUpdate(UpdateUserPhoto action, List<Chat> chats, List<User> users) {
        return Mono.fromRunnable(() -> {
            for (Chat chat : chats) {
                this.chats.put(chat.id(), chat);
            }

            users.forEach(this::saveUser);
        });
    }

    @Override
    public Mono<Void> onUserStatusUpdate(UpdateUserStatus action, List<Chat> chats, List<User> users) {
        return Mono.fromRunnable(() -> {
            for (Chat chat : chats) {
                this.chats.put(chat.id(), chat);
            }

            users.forEach(this::saveUser);
        });
    }

    @Override
    public Mono<Void> onUserTyping(UpdateUserTyping action, List<Chat> chats, List<User> users) {
        return Mono.fromRunnable(() -> {
            for (Chat chat : chats) {
                this.chats.put(chat.id(), chat);
            }

            users.forEach(this::saveUser);
        });
    }

    private void saveUser(User user) {
        if (user instanceof BaseUser) {
            BaseUser user0 = (BaseUser) user;
            users.put(user0.id(), user0);

            String username = user0.username();
            Long accessHash = user0.accessHash();
            // TODO: if access hash is null we must remove peer from cache?
            if (username != null && accessHash != null) {
                usernames.put(stripUsername(username), ImmutableInputPeerUser.of(user0.id(), accessHash));
            }
        }
    }

    static MessageId create(Message message) {
        if (message instanceof BaseMessage) {
            BaseMessage baseMessage = (BaseMessage) message;

            return new MessageId(baseMessage.id(), peerId(baseMessage.peerId()));
        }

        if (message instanceof MessageService) {
            MessageService messageService = (MessageService) message;

            return new MessageId(messageService.id(), peerId(messageService.peerId()));
        }

        throw new IllegalArgumentException("Unknown message: " + message);
    }

    static String stripUsername(String username) {
        String corrected = username.toLowerCase()
                .trim().replace(".", "");

        if (corrected.startsWith("@")) {
            return corrected.substring(1);
        }
        return corrected;
    }

    static class MessageId {
        private final int messageId;
        private final long chatId;

        MessageId(int messageId, long chatId) {
            this.messageId = messageId;
            this.chatId = chatId;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MessageId messageId1 = (MessageId) o;
            return messageId == messageId1.messageId && chatId == messageId1.chatId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(messageId, chatId);
        }
    }
}
