package telegram4j.core;

import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.inline.InlineQueryEvent;
import telegram4j.core.spec.AnswerInlineCallbackQuerySpec;
import telegram4j.core.spec.inline.InlineMessageMediaAutoSpec;
import telegram4j.core.spec.inline.InlineResultDocumentSpec;
import telegram4j.core.spec.inline.SizeSpec;
import telegram4j.core.spec.inline.WebDocumentSpec;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.store.StoreLayoutImpl;

import java.time.Duration;
import java.util.function.Function;

public class MTProtoBotInlineExample {

    private static final Duration CACHE_TIME = Duration.ofSeconds(30);

    public static void main(String[] args) {

        int apiId = Integer.parseInt(System.getenv("T4J_API_ID"));
        String apiHash = System.getenv("T4J_API_HASH");
        String botAuthToken = System.getenv("T4J_TOKEN");

        // Don't forget to enable inline queries in the @BotFather settings
        MTProtoTelegramClient.create(apiId, apiHash, botAuthToken)
                .setStoreLayout(new TestFileStoreLayout(new StoreLayoutImpl(Function.identity())))
                .withConnection(client -> {

                    Mono<Void> listenInline = client.on(InlineQueryEvent.class)
                            .filter(e -> e.getQuery().equals("getf"))
                            .flatMap(e -> e.answer(AnswerInlineCallbackQuerySpec.builder()
                                    .cacheTime(CACHE_TIME)
                                    .addResult(InlineResultDocumentSpec.builder()
                                            .id("1")
                                            .size(SizeSpec.of(498, 385))
                                            .type(FileReferenceId.DocumentType.VIDEO)
                                            .mimeType("video/mp4")
                                            .duration(Duration.ofMillis(800))
                                            .title("OWO!!!!")
                                            .thumb(WebDocumentSpec.of("https://github.com/Anuken/Mindustry/blob/master/core/assets-raw/sprites/blocks/campaign/interplanetary-accelerator.png?raw=true")
                                                    .withMimeType("image/jpeg"))
                                            .file("https://images-ext-1.discordapp.net/external/uyLYWYm7IVqYUFIgqcXoGm5JBHzGEel9UZQIY1d2b_k/https/c.tenor.com/VqUFZ4uNMCoAAAAM/niko-dance-one-shot-dancing.gif")
                                            .message(InlineMessageMediaAutoSpec.builder()
                                                    .message("nya~")
                                                    .build())
                                            .build())
                                    .build()))
                            .then();

                    return Mono.when(listenInline);
                })
                .block();
    }
}
