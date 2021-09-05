package telegram4j.core.spec;

import org.immutables.value.Value;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.core.object.MessageEntity;
import telegram4j.core.object.replymarkup.InlineKeyboardMarkup;
import telegram4j.core.object.replymarkup.ReplyMarkup;
import telegram4j.json.InputFile;
import telegram4j.json.ParseMode;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.SendDocument;
import telegram4j.rest.MultipartRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Value.Immutable
interface SendDocumentSpecGenerator extends Spec<MultipartRequest<SendDocument>> {

    ChatId chatId();

    Optional<InputFile> thumb();

    InputFile document();

    Optional<String> caption();

    Optional<ParseMode> parseMode();

    Optional<List<MessageEntity>> captionEntities();

    Optional<Boolean> disableContentTypeDetection();

    Optional<Boolean> disableNotification();

    Optional<Id> replyToMessageId();

    Optional<Boolean> allowSendingWithoutReply();

    Optional<ReplyMarkup> replyMarkup();

    @Override
    default MultipartRequest<SendDocument> asRequest() {
        SendDocument json = SendDocument.builder()
                .chatId(chatId())
                .caption(caption())
                .parseMode(parseMode())
                .captionEntities(captionEntities().map(list -> list.stream()
                        .map(MessageEntity::getData)
                        .collect(Collectors.toList())))
                .disableContentTypeDetection(disableContentTypeDetection())
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

        files.add(Tuples.of("document", document()));

        return MultipartRequest.ofBodyAndFiles(json, files);
    }
}
