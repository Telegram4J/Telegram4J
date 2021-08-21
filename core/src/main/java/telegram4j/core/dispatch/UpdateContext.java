package telegram4j.core.dispatch;

import telegram4j.core.TelegramClient;
import telegram4j.json.UpdateData;

public class UpdateContext {
    private final UpdateData updateData;
    private final TelegramClient client;

    public UpdateContext(UpdateData updateData, TelegramClient client) {
        this.updateData = updateData;
        this.client = client;
    }

    public UpdateData getUpdateData() {
        return updateData;
    }

    public TelegramClient getClient() {
        return client;
    }
}
