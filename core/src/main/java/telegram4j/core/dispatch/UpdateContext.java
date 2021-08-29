package telegram4j.core.dispatch;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.UpdateData;

import java.util.Optional;

public class UpdateContext<O> {
    private final UpdateData updateData;
    @Nullable
    private final O oldData;
    private final TelegramClient client;

    private UpdateContext(UpdateData updateData, @Nullable O oldData, TelegramClient client) {
        this.updateData = updateData;
        this.oldData = oldData;
        this.client = client;
    }

    public static <O> UpdateContext<O> of(UpdateData updateData, @Nullable O oldData, TelegramClient client) {
        return new UpdateContext<>(updateData, oldData, client);
    }

    public UpdateData getUpdateData() {
        return updateData;
    }

    @Nullable
    public O getOldData() {
        return oldData;
    }

    public TelegramClient getClient() {
        return client;
    }
}
