package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaDocument;

import java.time.Duration;
import java.util.Optional;

@Value.Immutable
interface InputMediaDocumentSpecDef extends InputMediaSpec {

    String document();

    Optional<String> query();

    Optional<Duration> autoDeleteDuration();

    @Override
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.fromCallable(() -> FileReferenceId.deserialize(document()).asInputDocument())
                .map(doc -> InputMediaDocument.builder()
                        .id(doc)
                        .query(query().orElse(null))
                        .ttlSeconds(autoDeleteDuration()
                                .map(Duration::getSeconds)
                                .map(Math::toIntExact)
                                .orElse(null))
                        .build());
    }
}
