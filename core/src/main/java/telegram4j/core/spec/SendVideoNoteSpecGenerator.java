package telegram4j.core.spec;

import org.immutables.value.Value;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.core.object.replymarkup.ReplyMarkup;
import telegram4j.json.InputFile;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.SendVideoNoteRequest;
import telegram4j.rest.MultipartRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Value.Immutable
interface SendVideoNoteSpecGenerator extends Spec<MultipartRequest<SendVideoNoteRequest>> {

    ChatId chatId();

    InputFile videoNote();

    Optional<Integer> duration();

    Optional<Integer> length();

    Optional<InputFile> thumb();

    Optional<Boolean> disableNotification();

    Optional<Id> replyToMessageId();

    Optional<Boolean> allowSendingWithoutReply();

    Optional<ReplyMarkup> replyMarkup();

    @Override
    default MultipartRequest<SendVideoNoteRequest> asRequest() {

        SendVideoNoteRequest json = SendVideoNoteRequest.builder()
                .chatId(chatId())
                .duration(duration())
                .length(length())
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

        files.add(Tuples.of("video_note", videoNote()));
        return MultipartRequest.ofBodyAndFiles(json, files);
    }
}
