package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

public class RestrictionReason implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.RestrictionReason data;

    public RestrictionReason(MTProtoTelegramClient client, telegram4j.tl.RestrictionReason data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public String getPlatform() {
        return data.platform();
    }

    public String getReason() {
        return data.reason();
    }

    public String getText() {
        return data.text();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestrictionReason that = (RestrictionReason) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "RestrictionReason{" +
                "data=" + data +
                '}';
    }
}
