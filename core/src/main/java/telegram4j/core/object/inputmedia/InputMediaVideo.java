package telegram4j.core.object.inputmedia;

import telegram4j.core.TelegramClient;
import telegram4j.json.InputFile;
import telegram4j.json.InputMediaData;

import java.util.Optional;

public class InputMediaVideo extends UploadInputMedia {

    InputMediaVideo(TelegramClient client, InputMediaData data, InputFile thumb) {
        super(client, data, thumb);
    }

    public Optional<Integer> getWidth() {
        return getData().width();
    }

    public Optional<Integer> getHeight() {
        return getData().height();
    }

    public Optional<Integer> getDuration() {
        return getData().duration();
    }

    public Optional<Boolean> isSupportsStreaming() {
        return getData().supportsStreaming();
    }

    @Override
    public String toString() {
        return "InputMediaVideo{} " + super.toString();
    }
}
