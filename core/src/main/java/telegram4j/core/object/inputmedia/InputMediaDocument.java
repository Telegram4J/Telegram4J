package telegram4j.core.object.inputmedia;

import telegram4j.core.TelegramClient;
import telegram4j.json.InputFile;
import telegram4j.json.InputMediaData;

import java.util.Optional;

public class InputMediaDocument extends UploadInputMedia {

    InputMediaDocument(TelegramClient client, InputMediaData data, InputFile thumb) {
        super(client, data, thumb);
    }

    public Optional<Boolean> isDisableContentTypeDetection() {
        return getData().disableContentTypeDetection();
    }

    @Override
    public String toString() {
        return "InputMediaDocument{} " + super.toString();
    }
}
