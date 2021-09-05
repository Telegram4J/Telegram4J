package telegram4j.core.spec;

import org.immutables.value.Value;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.object.replymarkup.ReplyMarkup;
import telegram4j.json.InputFile;
import telegram4j.json.ParseMode;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.SendPhotoRequest;
import telegram4j.rest.MultipartRequest;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
interface SendPhotoSpecGenerator extends Spec<MultipartRequest<SendPhotoRequest>> {

    ChatId chatId();

    InputFile photo();

    Optional<String> caption();

    Optional<ParseMode> parseMode();

    Optional<List<MessageEntity>> captionEntities();

    Optional<Boolean> disableNotification();

    Optional<Id> replyToMessageId();

    Optional<Boolean> allowSendingWithoutReply();

    Optional<ReplyMarkup> replyMarkup();

    @Override
    default MultipartRequest<SendPhotoRequest> asRequest() {
        SendPhotoRequest json = SendPhotoRequest.builder()
                .chatId(chatId())
                .caption(caption())
                .parseMode(parseMode())
                .captionEntities(captionEntities().map(list -> list.stream()
                        .map(MessageEntity::getData)
                        .collect(Collectors.toList())))
                .disableNotification(disableNotification())
                .replyToMessageId(replyToMessageId())
                .allowSendingWithoutReply(allowSendingWithoutReply())
                .replyMarkup(replyMarkup().map(ReplyMarkup::getData))
                .build();

        List<Tuple2<String, InputFile>> files = Collections.singletonList(Tuples.of("photo", photo()));

        return MultipartRequest.ofBodyAndFiles(json, files);
    }
}
