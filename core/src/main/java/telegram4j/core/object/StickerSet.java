package telegram4j.core.object;

import reactor.util.annotation.Nullable;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.object.media.PhotoSize;
import telegram4j.core.util.EntityFactory;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.ImmutableInputStickerSetID;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class StickerSet implements TelegramObject {
    private final MTProtoTelegramClient client;
    private final telegram4j.tl.StickerSet data;

    @Nullable
    private final String fileReferenceId;

    public StickerSet(MTProtoTelegramClient client, telegram4j.tl.StickerSet data) {
        this.client = Objects.requireNonNull(client, "client");
        this.data = Objects.requireNonNull(data, "data");

        this.fileReferenceId = Optional.ofNullable(data.thumbVersion())
                .map(version -> FileReferenceId.ofStickerSet(
                ImmutableInputStickerSetID.of(data.id(), data.accessHash()), version)
                .serialize())
                .orElse(null);
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public Optional<String> getFileReferenceId() {
        return Optional.ofNullable(fileReferenceId);
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

    public Optional<List<PhotoSize>> getThumbs() {
        return Optional.ofNullable(data.thumbs())
                .map(l -> l.stream()
                        .map(d -> EntityFactory.createPhotoSize(client, d))
                        .collect(Collectors.toList()));
    }

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
                ", fileReferenceId='" + fileReferenceId + '\'' +
                '}';
    }
}
