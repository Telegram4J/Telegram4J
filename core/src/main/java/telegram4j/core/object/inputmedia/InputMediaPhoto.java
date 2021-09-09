package telegram4j.core.object.inputmedia;

import telegram4j.core.TelegramClient;
import telegram4j.json.InputMediaData;

public class InputMediaPhoto extends InputMedia {

    InputMediaPhoto(TelegramClient client, InputMediaData data) {
        super(client, data);
    }

    @Override
    public String toString() {
        return "InputMediaPhoto{} " + super.toString();
    }
}
