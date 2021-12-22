package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class StickerSet implements TelegramObject {

    private final MTProtoTelegramClient client;
    private final telegram4j.tl.StickerSet data;

    public StickerSet(MTProtoTelegramClient client, telegram4j.tl.StickerSet data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public boolean isArchived() {
        return data.archived();
    }

    public boolean isOfficial() {
        return data.official();
    }

    public boolean isMasks() {
        return data.masks();
    }

    public boolean isAnimated() {
        return data.animated();
    }

    public Optional<Instant> getInstallTimestamp() {
        return Optional.ofNullable(data.installedDate()).map(Instant::ofEpochSecond);
    }

    public long getId() {
        return data.id();
    }

    public long getAccessHash() {
        return data.accessHash();
    }

    public String getTitle() {
        return data.title();
    }

    public String getShortName() {
        return data.shortName();
    }

    // @Nullable
    // public List<PhotoSize> getThumbs() {
    //     return data.thumbs();
    // }

    public Optional<Integer> getThumbDcId() {
        return Optional.ofNullable(data.thumbDcId());
    }

    public Optional<Integer> getThumbVersion() {
        return Optional.ofNullable(data.thumbVersion());
    }

    public int getCount() {
        return data.count();
    }

    public int getHash() {
        return data.hash();
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StickerSet that = (StickerSet) o;
        return data.equals(that.data);
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "StickerSet{" +
                "data=" + data +
                '}';
    }
}
