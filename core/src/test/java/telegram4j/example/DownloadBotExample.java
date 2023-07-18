package telegram4j.example;

import io.netty.util.ResourceLeakDetector;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.core.object.MessageMedia;
import telegram4j.core.object.MessageReplyHeader;
import telegram4j.core.object.chat.Chat;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.retriever.PreferredEntityRetriever;
import telegram4j.mtproto.MTProtoRetrySpec;
import telegram4j.mtproto.MethodPredicate;
import telegram4j.mtproto.ResponseTransformer;
import telegram4j.mtproto.store.FileStoreLayout;
import telegram4j.mtproto.store.StoreLayoutImpl;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.function.Function;

public class DownloadBotExample {
    // Example of bot that downloads any files sent in a dialog with it

    private static final Logger log = Loggers.getLogger(DownloadBotExample.class);

    public static void main(String[] args) throws IOException {

        // only for testing, do not copy it to your production code!!!
        Hooks.onOperatorDebug();
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        int apiId = Integer.parseInt(System.getenv("T4J_API_ID"));
        String apiHash = System.getenv("T4J_API_HASH");
        String botAuthToken = System.getenv("T4J_TOKEN");

        Path dir = Path.of("core/src/test/resources/stash");
        Files.createDirectories(dir);

        MTProtoTelegramClient.create(apiId, apiHash, botAuthToken)
                // prefer retrieving full data about peer entities
                .setEntityRetrieverStrategy(EntityRetrievalStrategy.preferred(
                        EntityRetrievalStrategy.STORE_FALLBACK_RPC, PreferredEntityRetriever.Setting.FULL,
                        PreferredEntityRetriever.Setting.FULL))
                .setStoreLayout(new FileStoreLayout(new StoreLayoutImpl(Function.identity()),
                        Path.of("core/src/test/resources/t4j-bot.bin")))
                .addResponseTransformer(ResponseTransformer.retryFloodWait(MethodPredicate.all(),
                        MTProtoRetrySpec.max(d -> d.getSeconds() < 30, 2)))
                .withConnection(client -> {

                    return client.on(SendMessageEvent.class)
                            // Listen only private chats
                            .filter(e -> e.getChat().map(c -> c.getType() == Chat.Type.PRIVATE).orElse(false))
                            // Reply to message with media or send new one
                            .flatMap(e -> e.getMessage().getReplyTo()
                                    .map(MessageReplyHeader::getMessage)
                                    .orElse(Mono.empty())
                                    .map(msg -> msg.getMessages().get(0))
                                    .defaultIfEmpty(e.getMessage()))
                            .flatMap(e -> Mono.justOrEmpty(e.getMedia()))
                            .ofType(MessageMedia.Document.class)
                            .flatMap(doc -> Mono.justOrEmpty(doc.getDocument()))
                            .flatMap(doc -> {
                                long t = System.currentTimeMillis();
                                Path filePath = dir.resolve(t + ".file");
                                log.info("| Downloading file {}", filePath);

                                return Mono.usingWhen(Mono.fromCallable(() -> FileChannel.open(filePath, StandardOpenOption.WRITE, StandardOpenOption.CREATE)),
                                                fc -> client.downloadFile(doc.getFileReferenceId())
                                                        .flatMap(fp -> Mono.fromCallable(() -> fc.write(fp.getBytes().nioBuffer())))
                                                        .then(),
                                                fc -> Mono.fromCallable(() -> {
                                                    fc.close();
                                                    return null;
                                                }))
                                        .then(Mono.fromRunnable(() -> log.info("| File downloaded {} ({}s)", filePath,
                                                (System.currentTimeMillis() - t) / 1000f)));
                            })
                            .then();
                })
                .block();
    }
}
