package telegram4j.core.object;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.ChatPhotoFields;

import java.util.Optional;

public class ChatPhoto implements TelegramObject {
    private final MTProtoTelegramClient client;
    private final ChatPhotoFields data;

    public ChatPhoto(MTProtoTelegramClient client, ChatPhotoFields data) {
        this.client = client;
        this.data = data;
    }

    @Override
    public MTProtoTelegramClient getClient() {
        return client;
    }

    public boolean hasVideo() {
        return data.hasVideo();
    }

    public long getPhotoId() {
        return data.photoId();
    }

    public Optional<byte[]> strippedThumb() {
        return Optional.ofNullable(data.strippedThumb());
    }

    public int getDc() {
        return data.dcId();
    }
}
