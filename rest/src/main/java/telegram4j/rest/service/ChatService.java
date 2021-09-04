package telegram4j.rest.service;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.json.ChatData;
import telegram4j.json.MessageData;
import telegram4j.json.PollData;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.*;
import telegram4j.rest.MultipartRequest;
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

    public Mono<Id> copyMessage(MessageCopy messageCopy) {
        return Routes.COPY_MESSAGE.newRequest()
                .body(messageCopy)
                .exchange(router)
                .bodyTo(Id.class);
    }

    public Mono<ChatData> getChat(ChatId chatId) {
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

    public Mono<JsonNode> editMessageText(MessageEditText messageEditText) {
        return Routes.EDIT_MESSAGE_TEXT.newRequest()
                .body(messageEditText)
                .exchange(router)
                .bodyTo(JsonNode.class);
    }

    public Mono<JsonNode> editMessageCaption(MessageEditCaption messageEditCaption) {
        return Routes.EDIT_MESSAGE_CAPTION.newRequest()
                .body(messageEditCaption)
                .exchange(router)
                .bodyTo(JsonNode.class);
    }

    public Mono<JsonNode> editMessageMedia(MultipartRequest<MessageEditMedia> messageEditMedia) {
        return Routes.EDIT_MESSAGE_MEDIA.newRequest()
                .header("content-type", "multipart/form-data")
                .body(messageEditMedia)
                .exchange(router)
                .bodyTo(JsonNode.class);
    }

    public Mono<JsonNode> editMessageReplyMarkup(MessageEditReplyMarkup messageEditReplyMarkup) {
        return Routes.EDIT_MESSAGE_REPLY_MARKUP.newRequest()
                .body(messageEditReplyMarkup)
                .exchange(router)
                .bodyTo(JsonNode.class);
    }

    public Mono<PollData> stopPoll(StopPoll stopPoll) {
        return Routes.STOP_POLL.newRequest()
                .body(stopPoll)
                .exchange(router)
                .bodyTo(PollData.class);
    }

    public Mono<MessageData> sendDocument(MultipartRequest<SendDocument> request) {
        return Routes.SEND_DOCUMENT.newRequest()
                .header("content-type", "multipart/form-data")
                .body(request)
                .exchange(router)
                .bodyTo(MessageData.class);
    }

    public Flux<MessageData> sendMediaGroup(MultipartRequest<SendMediaGroup> request) {
        return Routes.SEND_MEDIA_GROUP.newRequest()
                .header("content-type", "multipart/form-data")
                .body(request)
                .exchange(router)
                .bodyTo(MessageData[].class)
                .flatMapMany(Flux::fromArray);
    }
}
