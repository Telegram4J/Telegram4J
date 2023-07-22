package telegram4j.example;

import io.netty.util.ResourceLeakDetector;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.inline.InlineQueryEvent;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.object.MessageMedia;
import telegram4j.core.spec.AnswerInlineCallbackQuerySpec;
import telegram4j.core.spec.inline.InlineMessageSpec;
import telegram4j.core.spec.inline.InlineResultArticleSpec;
import telegram4j.core.spec.inline.InlineResultDocumentSpec;
import telegram4j.core.spec.markup.InlineButtonSpec;
import telegram4j.core.spec.markup.ReplyMarkupSpec;
import telegram4j.core.util.parser.EntityParserFactory;
import telegram4j.mtproto.file.FileReferenceId;
import telegram4j.mtproto.store.FileStoreLayout;
import telegram4j.mtproto.store.StoreLayoutImpl;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;

public class MTProtoBotInlineExample {

    private static final Duration CACHE_TIME = Duration.ofSeconds(30);
    private static final Duration PHOTO_CACHE_TIME = Duration.ofSeconds(5);

    private static volatile FileReferenceId lastPhotoId;

    public static void main(String[] args) {

        // only for testing, do not copy it to your production code!!!
        Hooks.onOperatorDebug();
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);



        int apiId = Integer.parseInt(System.getenv("T4J_API_ID"));
        String apiHash = System.getenv("T4J_API_HASH");
        String botAuthToken = System.getenv("T4J_TOKEN");

        // Don't forget to enable inline queries in the @BotFather settings
        MTProtoTelegramClient.create(apiId, apiHash, botAuthToken)
                .setStoreLayout(new FileStoreLayout(new StoreLayoutImpl(Function.identity()),
                        Path.of("core/src/test/resources/t4j-bot.bin")))
                .withConnection(client -> {

                    Mono<Void> listenInline = client.on(InlineQueryEvent.class)
                            .flatMap(MTProtoBotInlineExample::handleInlineQuery)
                            .then();

                    // listen all messages with documents/photos to update lastPhotoId
                    Mono<Void> listenIncomingFri = client.on(SendMessageEvent.class)
                            .flatMap(s -> Mono.justOrEmpty(s.getMessage().getMedia()))
                            .ofType(MessageMedia.Document.class)
                            .flatMap(d -> Mono.justOrEmpty(d.getDocument()))
                            .doOnNext(s -> lastPhotoId = s.getFileReferenceId())
                            .then();

                    return Mono.when(listenInline, listenIncomingFri);
                })
                .block();
    }

    private static Publisher<?> handleInlineQuery(InlineQueryEvent e) {
        switch (e.getQuery().toLowerCase()) {
            case "article" -> {
                return e.answer(AnswerInlineCallbackQuerySpec.builder()
                        .cacheTime(CACHE_TIME)
                        .addResult(InlineResultArticleSpec.builder()
                                .id("1")
                                .title("Telegram wikipedia page.")
                                .url("https://en.wikipedia.org/wiki/Telegram_(software)")
                                .message(InlineMessageSpec.text("[Telegram wikipedia page.](https://en.wikipedia.org/wiki/Telegram_\\(software\\))")
                                        .withParser(EntityParserFactory.MARKDOWN_V2))
                                .build())
                        .build());
            }
            case "gif" -> {
                return e.answer(AnswerInlineCallbackQuerySpec.builder()
                        .cacheTime(CACHE_TIME)
                        .addResult(InlineResultDocumentSpec.builder()
                                .id("4")
                                .type(FileReferenceId.DocumentType.GIF)
                                .title("Niko caramelldansen!")
                                .document("https://media.tenor.com/VqUFZ4uNMCoAAAAC/niko-dance-one-shot-dancing.gif")
                                .size(498, 373)
                                .mimeType("image/gif")
                                .duration(Duration.ofMillis(800))
                                .message(InlineMessageSpec.mediaAuto("<i>Cute gif animation, isn't it?</i>")
                                        .withParser(EntityParserFactory.HTML)
                                        .withReplyMarkup(ReplyMarkupSpec.inlineKeyboard(List.of(List.of(
                                                InlineButtonSpec.url("yes!", "https://www.youtube.com/watch?v=zvq9r6R6QAY"),
                                                InlineButtonSpec.url("no!", "https://www.youtube.com/watch?v=dQw4w9WgXcQ"))))))
                                .build())
                        .build());
            }
            case "photo" -> {
                return e.answer(AnswerInlineCallbackQuerySpec.builder()
                        .cacheTime(CACHE_TIME)
                        .addResult(InlineResultDocumentSpec.builder()
                                .id("5")
                                .type(FileReferenceId.DocumentType.PHOTO)
                                .size(256)
                                .document("https://raw.githubusercontent.com/telegramdesktop/tdesktop/dev/Telegram/Resources/art/icon256%402x.png")
                                .message(InlineMessageSpec.mediaAuto("Icon of TDesktop"))
                                .build())
                        .build());
            }
            case "lastphoto" -> {
                FileReferenceId photoId = lastPhotoId;
                if (photoId == null) {
                    return Mono.empty();
                }
                return e.answer(AnswerInlineCallbackQuerySpec.builder()
                        .cacheTime(PHOTO_CACHE_TIME)
                        .addResult(InlineResultDocumentSpec.builder()
                                .id("4")
                                .document(photoId)
                                .message(InlineMessageSpec.mediaAuto("_Hmm... It's a last photo which I saw_")
                                        .withParser(EntityParserFactory.MARKDOWN_V2))
                                .build())
                        .build());
            }
            default -> {
                return Mono.empty();
            }
        }
    }
}
