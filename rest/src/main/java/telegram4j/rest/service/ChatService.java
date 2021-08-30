package telegram4j.rest.service;

import reactor.core.publisher.Mono;
import telegram4j.json.ChatData;
import telegram4j.json.MessageData;
import telegram4j.json.MessageIdData;
import telegram4j.json.request.*;
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

    public Mono<ChatData> getChat(long chatId) {
        return Routes.GET_CHAT.newRequest()
                .body(GetChat.builder()
                        .chatId(chatId)
                        .build())
                .exchange(router)
                .bodyTo(ChatData.class);
    }

    public Mono<Boolean> deleteMessage(long chatId, long messageId) {
        return Routes.DELETE_MESSAGE.newRequest()
                .body(DeleteMessage.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .build())
                .exchange(router)
                .bodyTo(Boolean.class);
    }
}
