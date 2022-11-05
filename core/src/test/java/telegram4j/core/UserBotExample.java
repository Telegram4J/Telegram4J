package telegram4j.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.ResourceLeakDetector;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.CodeAuthorization;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.retriever.PreferredEntityRetriever;
import telegram4j.mtproto.store.StoreLayoutImpl;
import telegram4j.tl.json.TlModule;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

public class UserBotExample {

    private static final Logger log = Loggers.getLogger(UserBotExample.class);

    public static void main(String[] args) {

        // only for testing
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new TlModule());

        int apiId = 27277322;//Integer.parseInt(System.getenv("T4J_API_ID"));
        String apiHash = "8782058863c5321c89199ec620bb9aee";//System.getenv("T4J_API_HASH");

        MTProtoTelegramClient.create(apiId, apiHash, CodeAuthorization::authorize)
                .setEntityRetrieverStrategy(EntityRetrievalStrategy.preferred(
                        EntityRetrievalStrategy.STORE_FALLBACK_RPC, PreferredEntityRetriever.Setting.FULL, PreferredEntityRetriever.Setting.FULL))
                .setStoreLayout(new TestFileStoreLayout(new StoreLayoutImpl(Function.identity())))
                .withConnection(client -> {

                    Mono<Void> eventLog = client.getMtProtoClientGroup().main().updates().asFlux()
                            .publishOn(Schedulers.boundedElastic())
                            .flatMap(u -> Mono.fromCallable(() -> mapper.writeValueAsString(u)))
                            .doOnNext(log::info)
                            .then();

                    AtomicBoolean online = new AtomicBoolean(true);
                    Mono<Void> status = Flux.<Integer>create(sink -> {
                                var schedule = Schedulers.parallel().schedule(() -> {
                                    while (!Thread.currentThread().isInterrupted()) {
                                        try {
                                            int sleep = ThreadLocalRandom.current().nextInt(45, 160);
                                            log.info("Delaying {} status for {} seconds", online.getAcquire() ? "offline" : "online", sleep);
                                            TimeUnit.SECONDS.sleep(sleep);
                                            sink.next(1);
                                        } catch (InterruptedException e) {
                                            break;
                                        }
                                    }
                                });
                                sink.onCancel(schedule);
                            })
                            .flatMap(e -> {
                                boolean state = online.getAcquire();
                                online.setRelease(!state);
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
