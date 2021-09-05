package telegram4j.core.spec;

import org.immutables.value.Value;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.core.object.inputmedia.InputMedia;
import telegram4j.core.object.inputmedia.UploadInputMedia;
import telegram4j.core.object.replymarkup.InlineKeyboardMarkup;
import telegram4j.json.InputFile;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.MessageEditMediaRequest;
import telegram4j.rest.MultipartRequest;

import java.util.Optional;

import static telegram4j.core.spec.InternalSpecUtil.addAllOptional;

@Value.Immutable
interface MessageEditMediaSpecGenerator extends Spec<MultipartRequest<MessageEditMediaRequest>> {

    Optional<ChatId> chatId();

    Optional<Id> messageId();

    Optional<String> inlineMessageId();

    Optional<InputFile> mediaFile();

    InputMedia media();

    Optional<InlineKeyboardMarkup> replyMarkup();

    @Override
    default MultipartRequest<MessageEditMediaRequest> asRequest() {
        MessageEditMediaRequest json = MessageEditMediaRequest.builder()
                .chatId(chatId())
                .messageId(messageId())
                .inlineMessageId(inlineMessageId())
                .media(media().getData())
                .replyMarkup(replyMarkup().map(InlineKeyboardMarkup::getData))
                .build();

        Optional<Tuple2<String, InputFile>> mediaFile = mediaFile()
                .filter(f -> media().getMedia()
                        .map(m -> m.startsWith("attach://"))
                        .orElse(false))
                .map(file -> Tuples.of("media", file));

        Optional<Tuple2<String, InputFile>> thumbFile = Optional.of(media())
                .filter(media -> media instanceof UploadInputMedia)
                .map(media -> (UploadInputMedia) media)
                .flatMap(UploadInputMedia::getThumb)
                .map(file -> Tuples.of("thumb", file));

        return MultipartRequest.ofBodyAndFiles(json, addAllOptional(mediaFile, thumbFile));
    }
}
