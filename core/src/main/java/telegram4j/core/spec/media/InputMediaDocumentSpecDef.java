package telegram4j.core.spec.media;

import org.immutables.value.Value;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.tl.InputDocument;
import telegram4j.tl.InputMedia;
import telegram4j.tl.InputMediaDocument;
import telegram4j.tl.InputMediaDocumentExternal;

import java.time.Duration;
import java.util.Optional;

@Value.Immutable
interface InputMediaDocumentSpecDef extends InputMediaSpec {

    /**
     * @return The serialized {@link telegram4j.mtproto.file.FileReferenceId} or url to web file.
     */
    String document();

    /**
     * @return The indexing emoji/query for forwarded document.
     */
    Optional<String> query();

    Optional<Duration> autoDeleteDuration();

    @Override
    default Mono<InputMedia> asData(MTProtoTelegramClient client) {
        return Mono.fromCallable(() -> {
            Integer ttlSeconds = autoDeleteDuration()
                    .map(Duration::getSeconds)
                    .map(Math::toIntExact)
                    .orElse(null);

            try {
                InputDocument doc = FileReferenceId.deserialize(document()).asInputDocument();

                return InputMediaDocument.builder()
                        .id(doc)
                        .query(query().orElse(null))
                        .ttlSeconds(ttlSeconds)
                        .build();
            } catch (IllegalArgumentException t) {
                return InputMediaDocumentExternal.builder()
                        .url(document())
                        .ttlSeconds(ttlSeconds)
                        .build();
            }
        });
    }
}
