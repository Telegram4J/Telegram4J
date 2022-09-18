package telegram4j.core;

import reactor.core.publisher.Mono;
import telegram4j.core.event.domain.inline.InlineQueryEvent;
import telegram4j.core.spec.AnswerInlineCallbackQuerySpec;
import telegram4j.core.spec.inline.InlineMessageTextSpec;
import telegram4j.core.spec.inline.InlineResultArticleSpec;
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
                            .filter(e -> e.getQuery().equals("telegram"))
                            .flatMap(e -> e.answer(AnswerInlineCallbackQuerySpec.builder()
                                    .cacheTime(CACHE_TIME)
                                    .addResult(InlineResultArticleSpec.builder()
                                            .id("1")
                                            .title("Telegram wikipedia page.")
                                            .url("https://en.wikipedia.org/wiki/Telegram_(software)")
                                            .message(InlineMessageTextSpec.builder()
                                                    .message("Telegram wikipedia page.")
                                                    .build())
                                            .build())
                                    .build()))
                            .then();

                    return Mono.when(listenInline);
                })
                .block();
    }
}
