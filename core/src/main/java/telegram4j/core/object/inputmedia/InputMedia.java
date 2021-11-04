package telegram4j.core.object.inputmedia;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.object.TelegramObject;
import telegram4j.json.InputFile;
import telegram4j.json.InputMediaData;
import telegram4j.json.InputMediaType;
import telegram4j.json.ParseMode;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class InputMedia implements TelegramObject {

    private final TelegramClient client;
    private final InputMediaData data;

    InputMedia(TelegramClient client, InputMediaData data) {
        this.client = client;
        this.data = data;
    }

    public static InputMedia of(TelegramClient client, InputMediaData data) {
        switch (data.type()) {
            case VIDEO: return new InputMediaVideo(client, data, null);
            case DOCUMENT: return new InputMediaDocument(client, data, null);
            case ANIMATION: return new InputMediaAnimation(client, data, null);
            case AUDIO: return new InputMediaAudio(client, data, null);
            case PHOTO: return new InputMediaPhoto(client, data);
            default: throw new IllegalStateException("Unexpected value: " + data.type());
        }
    }

    public static UploadInputMedia ofThumb(TelegramClient client, InputMediaData data, @Nullable InputFile thumb) {
        switch (data.type()) {
            case VIDEO: return new InputMediaVideo(client, data, thumb);
            case DOCUMENT: return new InputMediaDocument(client, data, thumb);
            case ANIMATION: return new InputMediaAnimation(client, data, thumb);
            case AUDIO: return new InputMediaAudio(client, data, thumb);
            case PHOTO:
            default: throw new IllegalArgumentException("InputMediaPhoto is not allowed with input file.");
        }
    }

    @Override
    public TelegramClient getClient() {
        return client;
    }

    public InputMediaData getData() {
        return data;
    }

    public InputMediaType getType() {
        return data.type();
    }

    public Optional<String> getMedia() {
        return getData().media();
    }

    public Optional<String> getCaption() {
        return getData().caption();
    }

    public Optional<ParseMode> getParseMode() {
        return getData().parseMode();
    }

    public Optional<List<MessageEntity>> getCaptionEntities() {
        return getData().captionEntities().map(list -> list.stream()
                .map(data -> new MessageEntity(getClient(), data,
                        getCaption().orElseThrow(IllegalStateException::new)))
                .collect(Collectors.toList()));
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof InputMedia)) return false;
        InputMedia that = (InputMedia) o;
        return getData().equals(that.getData());
    }

    @Override
    public int hashCode() {
        return data.hashCode();
    }

    @Override
    public String toString() {
        return "BaseInputMedia{data=" + data + '}';
    }
}
