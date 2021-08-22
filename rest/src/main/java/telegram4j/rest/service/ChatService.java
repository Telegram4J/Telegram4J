package telegram4j.rest.service;

import reactor.core.publisher.Mono;
import telegram4j.json.MessageData;
import telegram4j.json.MessageIdData;
import telegram4j.json.request.MessageCopy;
import telegram4j.json.request.MessageCreate;
import telegram4j.json.request.MessageForward;
import telegram4j.rest.RestRouter;
import telegram4j.rest.route.Routes;

public class ChatService extends RestService {

    public ChatService(RestRouter router) {
        super(router);
    }

    public Mono<MessageData> sendMessage(MessageCreate messageCreate) {
        return Routes.SEND_MESSAGE.newRequest()
                .body(messageCreate)
                .exchange(router)
                .bodyTo(MessageData.class);
    }

    public Mono<MessageData> forwardMessage(MessageForward messageForward) {
        return Routes.FORWARD_MESSAGE.newRequest()
                .body(messageForward)
                .exchange(router)
                .bodyTo(MessageData.class);
    }

    public Mono<MessageIdData> copyMessage(MessageCopy messageCopy) {
        return Routes.COPY_MESSAGE.newRequest()
                .body(messageCopy)
                .exchange(router)
                .bodyTo(MessageIdData.class);
    }
}
