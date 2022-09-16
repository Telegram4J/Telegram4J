package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class PeerNotifySettings implements TelegramObject {
    private final MTProtoTelegramClient client;
    private final telegram4j.tl.PeerNotifySettings data;

    public PeerNotifySettings(MTProtoTelegramClient client, telegram4j.tl.PeerNotifySettings data) {
        this.client = Objects.requireNonNull(client);
        this.data = Objects.requireNonNull(data);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public Optional<Boolean> isShowPreviews() {
        return Optional.ofNullable(data.showPreviews());
    }

    public Optional<Boolean> isSilent() {
        return Optional.ofNullable(data.silent());
    }

    public Optional<Instant> getMuteUntilTimestamp() {
        return Optional.ofNullable(data.muteUntil()).map(Instant::ofEpochSecond);
    }

    // TODO: implement
    // public Optional<NotificationSound> getIosSound() {
    //     return Optional.empty();
    // }

    // public Optional<NotificationSound> getAndroidSound() {
    //     return Optional.empty();
    // }

    // public Optional<NotificationSound> getOtherSound() {
    //     return Optional.empty();
    // }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PeerNotifySettings that = (PeerNotifySettings) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "PeerNotifySettings{" +
                "data=" + data +
                '}';
    }
}
