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
import telegram4j.json.request.SendVideoRequest;
import telegram4j.rest.MultipartRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
interface SendVideoSpecGenerator extends Spec<MultipartRequest<SendVideoRequest>> {

    ChatId chatId();

    InputFile video();

    Optional<Integer> duration();

    Optional<Integer> width();

    Optional<Integer> height();

    Optional<String> caption();

    Optional<ParseMode> parseMode();

    Optional<List<MessageEntity>> captionEntities();

    Optional<InputFile> thumb();

    Optional<Boolean> supportsStreaming();

    Optional<Boolean> disableNotification();

    Optional<Id> replyToMessageId();

    Optional<Boolean> allowSendingWithoutReply();

    Optional<ReplyMarkup> replyMarkup();

    @Override
    default MultipartRequest<SendVideoRequest> asRequest() {

        SendVideoRequest json = SendVideoRequest.builder()
                .chatId(chatId())
                .duration(duration())
                .width(width())
                .height(height())
                .caption(caption())
                .parseMode(parseMode())
                .captionEntities(captionEntities().map(list -> list.stream()
                        .map(MessageEntity::getData)
                        .collect(Collectors.toList())))
                .supportsStreaming(supportsStreaming())
                .disableNotification(disableNotification())
                .replyToMessageId(replyToMessageId())
                .allowSendingWithoutReply(allowSendingWithoutReply())
                .replyMarkup(replyMarkup().map(ReplyMarkup::getData))
                .build();

        List<Tuple2<String, InputFile>> files = new ArrayList<>(1);
        if (thumb().isPresent()) {
            InputFile thumb = thumb().orElseThrow(IllegalStateException::new);
            files.add(Tuples.of("thumb", thumb));
        }

        files.add(Tuples.of("video", video()));
        return MultipartRequest.ofBodyAndFiles(json, files);
    }
}
