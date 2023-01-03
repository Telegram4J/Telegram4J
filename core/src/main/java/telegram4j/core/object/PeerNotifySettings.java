package telegram4j.core.object;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class PeerNotifySettings {
    private final telegram4j.tl.PeerNotifySettings data;

    public PeerNotifySettings(telegram4j.tl.PeerNotifySettings data) {
        this.data = Objects.requireNonNull(data);
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
    public String toString() {
        return "PeerNotifySettings{" +
                "data=" + data +
                '}';
    }
}
