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

import static telegram4j.mtproto.util.TlEntityUtil.getRawPeerId;

public class StoreLayoutImpl implements StoreLayout {

    private final ConcurrentMap<MessageId, Message> messages = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, PartialFields<Chat, ChatFull>> chats = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, PartialFields<ImmutableBaseUser, ImmutableUserFull>> users = new ConcurrentHashMap<>();
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
    public Mono<Chat> getChatMinById(long chatId) {
        return Mono.fromSupplier(() -> chats.get(chatId)).map(PartialFields::getMin);
    }

    @Override
    public Mono<ChatFull> getChatFullById(long chatId) {
        return Mono.fromSupplier(() -> chats.get(chatId)).mapNotNull(PartialFields::getFull);
    }

    @Override
    public Mono<User> getUserMinById(long userId) {
        return Mono.fromSupplier(() -> users.get(userId)).map(PartialFields::getMin);
    }

    @Override
    public Mono<UserFull> getUserFullById(long userId) {
        return Mono.fromSupplier(() -> users.get(userId)).mapNotNull(PartialFields::getFull);
    }

    @Override
    public Mono<AuthorizationKeyHolder> getAuthorizationKey(DataCenter dc) {
        return Mono.fromSupplier(() -> authorizationKeys.get(dc));
    }

    @Override
    public Mono<Void> onNewMessage(Message message, List<Chat> chats, List<User> users) {
        return Mono.fromRunnable(() -> {
            saveContacts(chats, users);
            messages.put(createMessageId(message), message);
        });
    }

    @Override
    public Mono<Message> onEditMessage(Message message, List<Chat> chats, List<User> users) {
        return Mono.fromSupplier(() -> {
            saveContacts(chats, users);
            return messages.put(createMessageId(message), message);
        });
    }

    @Override
    public Mono<Void> onChannelUserTyping(UpdateChannelUserTyping action, List<Chat> chats, List<User> users) {
        return Mono.fromRunnable(() -> {
            saveContacts(chats, users);
        });
    }

    @Override
    public Mono<Void> onChatUserTyping(UpdateChatUserTyping action, List<Chat> chats, List<User> users) {
        return Mono.fromRunnable(() -> {
            saveContacts(chats, users);
        });
    }

    @Override
    public Mono<Void> onUserTyping(UpdateUserTyping action, List<Chat> chats, List<User> users) {
        return Mono.fromRunnable(() -> {
            saveContacts(chats, users);
        });
    }

    @Override
    public Mono<UserNameFields> onUserNameUpdate(UpdateUserName action, List<Chat> chats, List<User> users) {
        return Mono.fromSupplier(() -> {
            saveContacts(chats, users);
            var old = this.users.get(action.userId());
            UserNameFields fields = old != null ? new UserNameFields(old.min.username(), old.min.firstName(), old.min.lastName()) : null;
            this.users.computeIfPresent(action.userId(), (k, v) -> v.withMin(v.min
                    .withUsername(action.username())
                    .withFirstName(action.firstName())
                    .withLastName(action.lastName())));

            return fields;
        });
    }

    @Override
    public Mono<String> onUserPhoneUpdate(UpdateUserPhone action, List<Chat> chats, List<User> users) {
        return Mono.fromSupplier(() -> {
            saveContacts(chats, users);
            var old = this.users.get(action.userId());
            this.users.computeIfPresent(action.userId(), (k, v) -> v.withMin(v.min.withPhone(action.phone())));

            return old != null ? old.min.phone() : null;
        });
    }

    @Override
    public Mono<UserProfilePhoto> onUserPhotoUpdate(UpdateUserPhoto action, List<Chat> chats, List<User> users) {
        return Mono.fromSupplier(() -> {
            saveContacts(chats, users);
            var old = this.users.get(action.userId());
            this.users.computeIfPresent(action.userId(), (k, v) -> v.withMin(v.min.withPhoto(action.photo())));

            return old != null ? old.min.photo() : null;
        });
    }

    @Override
    public Mono<UserStatus> onUserStatusUpdate(UpdateUserStatus action, List<Chat> chats, List<User> users) {
        return Mono.fromSupplier(() -> {
            saveContacts(chats, users);
            var old = this.users.get(action.userId());
            this.users.computeIfPresent(action.userId(), (k, v) -> v.withMin(v.min.withStatus(action.status())));

            return old != null ? old.min.status() : null;
        });
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
    public Mono<UserFull> onUserUpdate(UserFull payload) {
        return Mono.fromSupplier(() -> users.put(payload.user().id(),
                new PartialFields<>(ImmutableBaseUser.copyOf((BaseUser) payload.user()),
                        ImmutableUserFull.copyOf(payload))))
                .mapNotNull(PartialFields::getFull);
    }

    private void saveUser(User user) {
        if (user.identifier() == BaseUser.ID) {
            ImmutableBaseUser user0 = ImmutableBaseUser.copyOf((BaseUser) user);
            users.put(user0.id(), new PartialFields<>(user0));

            String username = user0.username();
            Long accessHash = user0.accessHash();
            // TODO: if access hash is null we must remove peer from cache?
            if (username != null && accessHash != null) {
                usernames.put(stripUsername(username), ImmutableInputPeerUser.of(user0.id(), accessHash));
            }
        }
    }

    private void saveChat(Chat chat) {
        Chat cpy;
        switch (chat.identifier()) {
            case BaseChat.ID:
                cpy = ImmutableBaseChat.copyOf((BaseChat) chat);
                break;
            case Channel.ID:
                cpy = ImmutableChannel.copyOf((Channel) chat);
                break;
            default:
                throw new IllegalArgumentException("Unknown chat type: " + chat);
        }

        chats.put(chat.id(), new PartialFields<>(cpy));
    }

    private void saveContacts(List<Chat> chats, List<User> users) {
        for (Chat chat : chats) {
            saveChat(chat);
        }

        for (User user : users) {
            saveUser(user);
        }
    }

    static MessageId createMessageId(Message message) {
        switch (message.identifier()) {
            case BaseMessage.ID:
                BaseMessage baseMessage = (BaseMessage) message;

                return new MessageId(baseMessage.id(), getRawPeerId(baseMessage.peerId()));
            case MessageService.ID:
                MessageService messageService = (MessageService) message;

                return new MessageId(messageService.id(), getRawPeerId(messageService.peerId()));
            default:
                throw new IllegalArgumentException("Unknown message: " + message);
        }
    }

    static String stripUsername(String username) {
        return username.toLowerCase().trim()
                .replace(".", "")
                .replace("@", "");
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

    static class PartialFields<M, F> {
        private final M min;
        @Nullable
        private final F full;

        PartialFields(M min) {
            this(min, null);
        }

        PartialFields(M min, @Nullable F full) {
            this.min = min;
            this.full = full;
        }

        public M getMin() {
            return min;
        }

        @Nullable
        public F getFull() {
            return full;
        }

        public PartialFields<M, F> withMin(M min) {
            if (Objects.equals(this.min, min)) {
                return this;
            }
            return new PartialFields<>(min, full);
        }

        public PartialFields<M, F> withFull(@Nullable F full) {
            if (Objects.equals(this.full, full)) {
                return this;
            }
            return new PartialFields<>(min, full);
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PartialFields<?, ?> that = (PartialFields<?, ?>) o;
            return min.equals(that.min) && Objects.equals(full, that.full);
        }

        @Override
        public int hashCode() {
            return Objects.hash(min, full);
        }
    }
}
