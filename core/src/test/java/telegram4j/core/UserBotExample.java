package telegram4j.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.util.ResourceLeakDetector;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.CodeAuthorization;
import telegram4j.core.retriever.EntityRetrievalStrategy;
import telegram4j.core.retriever.PreferredEntityRetriever;
import telegram4j.mtproto.store.StoreLayoutImpl;
import telegram4j.tl.json.TlModule;

import java.util.function.Function;

public class UserBotExample {

    private static final Logger log = Loggers.getLogger(UserBotExample.class);

    public static void main(String[] args) {

        // only for testing
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new TlModule());

        int apiId = Integer.parseInt(System.getenv("T4J_API_ID"));
        String apiHash = System.getenv("T4J_API_HASH");

        MTProtoTelegramClient.create(apiId, apiHash, CodeAuthorization::authorize)
                .setEntityRetrieverStrategy(EntityRetrievalStrategy.preferred(
                        EntityRetrievalStrategy.STORE_FALLBACK_RPC, PreferredEntityRetriever.Setting.FULL, PreferredEntityRetriever.Setting.FULL))
                .setStoreLayout(new TestFileStoreLayout(new StoreLayoutImpl(Function.identity())))
                .withConnection(client -> {

                    Mono<Void> eventLog = client.getMtProtoClient().updates().asFlux()
                            .publishOn(Schedulers.boundedElastic())
                            .flatMap(u -> Mono.fromCallable(() -> mapper.writeValueAsString(u)))
                            .doOnNext(log::info)
                            .then();

                    return Mono.when(eventLog);
                })
                .block();
    }
}
