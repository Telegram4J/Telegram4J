package telegram4j.rest.service;

import reactor.core.publisher.Mono;
import reactor.util.annotation.Nullable;
import telegram4j.json.*;
import telegram4j.rest.RestRouter;
import telegram4j.rest.route.Routes;

import java.util.List;

public class ChatService extends RestService {

    public ChatService(RestRouter router) {
        super(router);
    }

    public Mono<MessageData> sendMessage(long chatId, String text, @Nullable ParseMode parseMode,
                                         @Nullable List<MessageEntityData> entities, @Nullable Boolean disableWebPreview,
                                         @Nullable Boolean disableNotification, @Nullable Long replyToMessageId,
                                         @Nullable Boolean allowSendingWithoutReply, @Nullable ReplyMarkup replyMarkup) {

        return Routes.SEND_MESSAGE.newRequest()
                .parameter("chat_id", chatId)
                .parameter("text", text)
                .optionalParameter("parse_mode", parseMode)
                .optionalParameter("entities", entities)
                .optionalParameter("disable_web_preview", disableWebPreview)
                .optionalParameter("disable_notification", disableNotification)
                .optionalParameter("reply_to_message_id", replyToMessageId)
                .optionalParameter("allow_sending_without_reply", allowSendingWithoutReply)
                .optionalParameter("reply_markup", replyMarkup)
                .exchange(router)
                .bodyTo(MessageData.class);
    }

    public Mono<MessageData> forwardMessage(long chatId, long fromChatId,
                                            @Nullable Boolean disableNotification, long messageId) {

        return Routes.FORWARD_MESSAGE.newRequest()
                .parameter("chat_id", chatId)
                .parameter("from_chat_id", fromChatId)
                .optionalParameter("disable_notification", disableNotification)
                .parameter("message_id", messageId)
                .exchange(router)
                .bodyTo(MessageData.class);
    }

    public Mono<MessageIdData> forwardMessage(long chatId, long fromChatId, long messageId,
                                              @Nullable String caption, @Nullable ParseMode parseMode,
                                              @Nullable List<MessageEntityData> captionEntities,
                                              @Nullable Boolean disableNotification, @Nullable Long replyToMessageId,
                                              @Nullable Boolean allowSendingWithoutReply, @Nullable ReplyMarkup replyMarkup) {

        return Routes.FORWARD_MESSAGE.newRequest()
                .parameter("chat_id", chatId)
                .parameter("from_chat_id", fromChatId)
                .parameter("message_id", messageId)
                .optionalParameter("caption", caption)
                .optionalParameter("parse_mode", parseMode)
                .optionalParameter("caption_entities", captionEntities)
                .optionalParameter("disable_notification", disableNotification)
                .optionalParameter("reply_to_message_id", replyToMessageId)
                .optionalParameter("allow_sending_without_reply", allowSendingWithoutReply)
                .optionalParameter("reply_markup", replyMarkup)
                .exchange(router)
                .bodyTo(MessageIdData.class);
    }
}
