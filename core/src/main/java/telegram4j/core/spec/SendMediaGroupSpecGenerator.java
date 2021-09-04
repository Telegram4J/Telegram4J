package telegram4j.core.spec;

import org.immutables.value.Value;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import telegram4j.core.object.inputmedia.InputMedia;
import telegram4j.core.object.inputmedia.UploadInputMedia;
import telegram4j.json.InputFile;
import telegram4j.json.api.ChatId;
import telegram4j.json.api.Id;
import telegram4j.json.request.SendMediaGroup;
import telegram4j.rest.MultipartRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Value.Immutable
interface SendMediaGroupSpecGenerator extends Spec<MultipartRequest<SendMediaGroup>> {

    ChatId chatId();

    List<InputMedia> media();

    List<SendMediaFields.File> attachments();

    Optional<Boolean> disableNotification();

    Optional<Id> replyToMessageId();

    Optional<Boolean> allowSendingWithoutReply();

    @Override
    default MultipartRequest<SendMediaGroup> asRequest() {
        SendMediaGroup json = SendMediaGroup.builder()
                .chatId(chatId())
                .media(media().stream()
                        .map(InputMedia::getData)
                        .collect(Collectors.toList()))
                .disableNotification(disableNotification())
                .replyToMessageId(replyToMessageId())
                .allowSendingWithoutReply(allowSendingWithoutReply())
                .build();

        List<Tuple2<String, InputFile>> mediaFiles = media().stream()
                .filter(media -> media instanceof UploadInputMedia)
                .map(media -> (UploadInputMedia) media)
                .flatMap(file -> file.getThumb().map(Stream::of).orElseGet(Stream::empty))
                .map(file -> Tuples.of("thumb", file)) // InputMedia file always named as 'thumb'
                .collect(Collectors.toList());

        mediaFiles.addAll(attachments().stream()
                .map(SendMediaFields.File::asRequest)
                .collect(Collectors.toList()));

        return MultipartRequest.ofBodyAndFiles(json, mediaFiles);
    }
}
