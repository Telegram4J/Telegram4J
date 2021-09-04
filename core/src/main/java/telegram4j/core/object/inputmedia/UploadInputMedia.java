package telegram4j.core.object.inputmedia;

import reactor.util.annotation.Nullable;
import telegram4j.core.TelegramClient;
import telegram4j.json.InputFile;
import telegram4j.json.InputMediaData;

import java.util.Optional;

public class UploadInputMedia extends InputMedia {

    @Nullable
    private final InputFile thumb;

    UploadInputMedia(TelegramClient client, InputMediaData data, @Nullable InputFile thumb) {
        super(client, data);
        this.thumb = thumb;
    }

    public Optional<InputFile> getThumb() {
        return Optional.ofNullable(thumb);
    }

    @Override
    public String toString() {
        return "UploadInputMedia{" +
                "thumb=" + thumb +
                "} " + super.toString();
    }
}
