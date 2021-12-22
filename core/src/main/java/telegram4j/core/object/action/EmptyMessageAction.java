package telegram4j.core.object.action;

import telegram4j.core.MTProtoTelegramClient;

public class EmptyMessageAction extends BaseMessageAction {

    public EmptyMessageAction(MTProtoTelegramClient client, Type type) {
        super(client, type);
    }
}
