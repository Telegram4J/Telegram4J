package telegram4j.core.store.impl;

import reactor.core.publisher.Mono;
import telegram4j.core.store.StoreLayout;
import telegram4j.json.ImmutableMessageData;
import telegram4j.json.ImmutableUserData;
import telegram4j.json.MessageData;
import telegram4j.json.UserData;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class LocalStoreLayout implements StoreLayout {
    private final ConcurrentMap<Long128, MessageData> messages = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, UserData> users = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> onMessageCreate(MessageData dispatch) {
        return Mono.fromRunnable(() -> {
            ImmutableMessageData message = ImmutableMessageData.copyOf(dispatch);
            Long128 id = Long128.of(message.chat().id(), message.messageId());
            messages.put(id, message);
        });
    }

    @Override
    public Mono<MessageData> onMessageUpdate(MessageData dispatch) {
        return Mono.fromSupplier(() -> {
            ImmutableMessageData edited = ImmutableMessageData.copyOf(dispatch);
            Long128 id = Long128.of(edited.chat().id(), edited.messageId());
            MessageData old = messages.get(id);
            messages.computeIfPresent(id, (key, data) -> edited);
            return old;
        });
    }

    @Override
    public Mono<MessageData> onMessageDelete(MessageData dispatch) {
        return Mono.fromSupplier(() -> messages.remove(Long128.of(dispatch.chat().id(), dispatch.messageId())));
    }

    @Override
    public Mono<UserData> onUserUpdate(UserData dispatch) {
        return Mono.fromSupplier(() -> {
            ImmutableUserData edited = ImmutableUserData.copyOf(dispatch);
            UserData old = users.get(edited.id());
            users.computeIfPresent(edited.id(), (key, data) -> edited);
            return old;
        });
    }

    @Override
    public Mono<MessageData> getMessageById(long chatId, long messageId) {
        return Mono.fromSupplier(() -> messages.get(Long128.of(chatId, messageId)));
    }

    @Override
    public Mono<UserData> getUserById(long userId) {
        return Mono.fromSupplier(() -> users.get(userId));
    }

    static final class Long128 {
        private final long t1;
        private final long t2;

        private Long128(long t1, long t2) {
            this.t1 = t1;
            this.t2 = t2;
        }

        static Long128 of(long t1, long t2) {
            return new Long128(t1, t2);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Long128 that = (Long128) o;
            return t1 == that.t1 && t2 == that.t2;
        }

        @Override
        public int hashCode() {
            return Objects.hash(t1, t2);
        }
    }
}
