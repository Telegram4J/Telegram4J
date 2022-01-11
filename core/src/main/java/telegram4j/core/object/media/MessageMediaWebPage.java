package telegram4j.core.object.media;

import telegram4j.core.MTProtoTelegramClient;
import telegram4j.tl.WebPage;

import java.util.Objects;

public class MessageMediaWebPage extends BaseMessageMedia {

    private final telegram4j.tl.MessageMediaWebPage data;

    public MessageMediaWebPage(MTProtoTelegramClient client, telegram4j.tl.MessageMediaWebPage data) {
        super(client, Type.WEB_PAGE);
        this.data = Objects.requireNonNull(data, "data");
    }

    // TODO
    public WebPage webpage() {
        return data.webpage();
    }
}
