package telegram4j.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.ResourceLeakDetector;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.MTProtoTelegramClient;
import telegram4j.core.event.DefaultUpdatesManager;
import telegram4j.core.event.DefaultUpdatesManager.Options;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.retriever.PreferredEntityRetriever;
import telegram4j.example.auth.CodeAuthorization;
import telegram4j.example.auth.QrEncodeCodeAuthorization;
import telegram4j.mtproto.MTProtoRetrySpec;
import telegram4j.mtproto.MethodPredicate;
import telegram4j.mtproto.ResponseTransformer;
import telegram4j.mtproto.store.FileStoreLayout;
import telegram4j.mtproto.store.StoreLayoutImpl;
import telegram4j.tl.json.TlModule;

import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class UserBotExample {

    private static final Logger log = Loggers.getLogger(UserBotExample.class);

    public static void main(String[] args) {
        // only for testing
        Hooks.onOperatorDebug();
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new TlModule());

        int apiId = Integer.parseInt(System.getenv("TEST_API_ID"));
        String apiHash = System.getenv("TEST_API_HASH");

        MTProtoTelegramClient.create(apiId, apiHash,
                        Boolean.getBoolean("useQrAuth")
                                ? QrEncodeCodeAuthorization::authorize
                                : CodeAuthorization::authorize)
                .setEntityRetrieverStrategy(EntityRetrievalStrategy.preferred(
                        EntityRetrievalStrategy.STORE_FALLBACK_RPC, PreferredEntityRetriever.Setting.FULL,
                        PreferredEntityRetriever.Setting.FULL))
                .setStoreLayout(new FileStoreLayout(new StoreLayoutImpl(Function.identity()),
                        Path.of("core/src/test/resources/t4j.bin")))
                .addResponseTransformer(ResponseTransformer.retryFloodWait(MethodPredicate.all(),
                        MTProtoRetrySpec.max(d -> d.getSeconds() < 30, Long.MAX_VALUE)))
                .setUpdatesManager(client -> new DefaultUpdatesManager(client,
                        new Options(Options.DEFAULT_CHECKIN,
                                Options.MAX_USER_CHANNEL_DIFFERENCE,
                                true)))
                .withConnection(client -> {

                    Mono<Void> eventLog = client.getMtProtoClientGroup().updates().all()
                            .flatMap(u -> Mono.fromCallable(() -> mapper.writeValueAsString(u)))
                            .doOnNext(log::info)
                            .then();

                    var online = new boolean[]{false};
                    // update user status in fixed random interval
                    Mono<Void> status = Flux.<Integer>create(sink -> {
                                var scheduler = Schedulers.newSingle("t4j-user-status");
                                var task = scheduler.schedule(() -> {
                                    while (!Thread.currentThread().isInterrupted()) {
                                        try {
                                            int sleep = ThreadLocalRandom.current().nextInt(45, 160);
                                            log.info("Delaying {} status for {} seconds", online[0] ? "offline" : "online", sleep);
                                            TimeUnit.SECONDS.sleep(sleep);
                                            sink.next(1);
                                        } catch (InterruptedException e) {
                                            sink.complete();
                                            break;
                                        }
                                    }
                                });
                                sink.onCancel(Disposables.composite(scheduler, task));
                            })
                            .flatMap(e -> {
                                boolean state = online[0];
                                online[0] = !state;
                                return client.getServiceHolder()
                                        .getAccountService()
                                        .updateStatus(state);
                            })
                            .then();

                    return Mono.when(eventLog, status);
                })
                .block();
    }
}
