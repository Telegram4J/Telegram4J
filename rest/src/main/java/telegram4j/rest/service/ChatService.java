package telegram4j.rest.service;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.json.ChatData;
import telegram4j.json.FileData;
import telegram4j.json.MessageData;
import telegram4j.json.PollData;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.*;
import telegram4j.rest.MultipartRequest;
import telegram4j.rest.RestRouter;
import telegram4j.rest.route.Routes;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ChatService extends RestService {

    public ChatService(RestRouter router) {
        super(router);
    }

    public Mono<MessageData> sendMessage(MessageCreateRequest request) {
        return Routes.SEND_MESSAGE.newRequest()
                .body(request)
                .exchange(router)
                .bodyTo(MessageData.class);
    }

    public Mono<MessageData> forwardMessage(MessageForwardRequest request) {
        return Routes.FORWARD_MESSAGE.newRequest()
                .body(request)
                .exchange(router)
                .bodyTo(MessageData.class);
    }

    public Mono<Id> copyMessage(MessageCopyRequest request) {
        return Routes.COPY_MESSAGE.newRequest()
                .body(request)
                .exchange(router)
                .bodyTo(Id.class);
    }

    public Mono<ChatData> getChat(ChatId chatId) {
        return Routes.GET_CHAT.newRequest()
                .body(Collections.singletonMap("chat_id", chatId))
                .exchange(router)
                .bodyTo(ChatData.class);
    }

    public Mono<Boolean> deleteMessage(ChatId chatId, Id messageId) {
        Map<String, Object> params = new HashMap<>(2);
        params.put("chat_id", chatId);
        params.put("message_id", messageId);

        return Routes.DELETE_MESSAGE.newRequest()
                .body(params)
                .exchange(router)
                .bodyTo(Boolean.class);
    }

    public Mono<JsonNode> editMessageText(MessageEditTextRequest request) {
        return Routes.EDIT_MESSAGE_TEXT.newRequest()
                .body(request)
                .exchange(router)
                .bodyTo(JsonNode.class);
    }

    public Mono<JsonNode> editMessageCaption(MessageEditCaptionRequest request) {
        return Routes.EDIT_MESSAGE_CAPTION.newRequest()
                .body(request)
                .exchange(router)
                .bodyTo(JsonNode.class);
    }

    public Mono<JsonNode> editMessageMedia(MultipartRequest<MessageEditMediaRequest> request) {
        return Routes.EDIT_MESSAGE_MEDIA.newRequest()
                .header("content-type", "multipart/form-data")
                .body(request)
                .exchange(router)
                .bodyTo(JsonNode.class);
    }

    public Mono<JsonNode> editMessageReplyMarkup(MessageEditReplyMarkupRequest request) {
        return Routes.EDIT_MESSAGE_REPLY_MARKUP.newRequest()
                .body(request)
                .exchange(router)
                .bodyTo(JsonNode.class);
    }

    public Mono<PollData> stopPoll(StopPollRequest stopPoll) {
        return Routes.STOP_POLL.newRequest()
                .body(stopPoll)
                .exchange(router)
                .bodyTo(PollData.class);
    }

    public Mono<MessageData> sendDocument(MultipartRequest<SendDocumentRequest> request) {
        return Routes.SEND_DOCUMENT.newRequest()
                .header("content-type", "multipart/form-data")
                .body(request)
                .exchange(router)
                .bodyTo(MessageData.class);
    }

    public Mono<MessageData> sendAudio(MultipartRequest<SendAudioRequest> request) {
        return Routes.SEND_AUDIO.newRequest()
                .header("content-type", "multipart/form-data")
                .body(request)
                .exchange(router)
                .bodyTo(MessageData.class);
    }

    public Mono<MessageData> sendVideo(MultipartRequest<SendVideoRequest> request) {
        return Routes.SEND_VIDEO.newRequest()
                .header("content-type", "multipart/form-data")
                .body(request)
                .exchange(router)
                .bodyTo(MessageData.class);
    }

    public Mono<MessageData> sendVideoNote(MultipartRequest<SendVideoNoteRequest> request) {
        return Routes.SEND_VIDEO_NOTE.newRequest()
                .header("content-type", "multipart/form-data")
                .body(request)
                .exchange(router)
                .bodyTo(MessageData.class);
    }

    public Mono<MessageData> sendPhoto(MultipartRequest<SendPhotoRequest> request) {
        return Routes.SEND_PHOTO.newRequest()
                .header("content-type", "multipart/form-data")
                .body(request)
                .exchange(router)
                .bodyTo(MessageData.class);
    }

    public Flux<MessageData> sendMediaGroup(MultipartRequest<SendMediaGroupRequest> request) {
        return Routes.SEND_MEDIA_GROUP.newRequest()
                .header("content-type", "multipart/form-data")
                .body(request)
                .exchange(router)
                .bodyTo(MessageData[].class)
                .flatMapMany(Flux::fromArray);
    }

    public Mono<FileData> getFile(String fileId) {
        return Routes.GET_FILE.newRequest()
                .body(Collections.singletonMap("file_id", fileId))
                .exchange(router)
                .bodyTo(FileData.class);
    }
}
