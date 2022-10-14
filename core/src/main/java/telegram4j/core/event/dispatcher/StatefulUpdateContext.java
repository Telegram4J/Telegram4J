package telegram4j.core.event.dispatcher;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.User;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.util.Id;
import telegram4j.tl.Update;

import java.util.Map;

public class StatefulUpdateContext<U extends Update, O> extends UpdateContext<U> {
    @Nullable
    private final O old;

    protected StatefulUpdateContext(MTProtoTelegramClient client, Map<Id, Chat> chats,
                                    Map<Id, User> users, U update, @Nullable O old) {
        super(client, chats, users, update);
        this.old = old;
    }

    public static <U extends Update, O> StatefulUpdateContext<U, O> from(UpdateContext<U> context, @Nullable O old) {
        return new StatefulUpdateContext<>(context.getClient(), context.getChats(),
                context.getUsers(), context.getUpdate(), old);
    }

    @Nullable
    public O getOld() {
        return old;
    }
}
