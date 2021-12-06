package telegram4j.core;

import io.netty.buffer.ByteBufAllocator;
import io.netty.util.ResourceLeakDetector;
import reactor.core.publisher.Mono;
import reactor.util.Logger;
import reactor.util.Loggers;
import telegram4j.core.event.domain.message.SendMessageEvent;
import telegram4j.mtproto.store.StoreLayoutImpl;

public class MTProtoClientExample {

    private static final Logger log = Loggers.getLogger(MTProtoClientExample.class);

    public static void main(String[] args) {

        int apiId = Integer.parseInt(System.getenv("T4J_API_ID"));
        String apiHash = System.getenv("T4J_API_HASH");
        String botAuthToken = System.getenv("T4J_TOKEN");

        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        MTProtoTelegramClient.create(apiId, apiHash, botAuthToken)
                .setStoreLayout(new TestFileStoreLayout(ByteBufAllocator.DEFAULT, new StoreLayoutImpl()))
                .withConnection(client -> {
                    Mono<Void> listenMessages = client.on(SendMessageEvent.class)
                            .doOnNext(e -> log.info("Received message event: {}", e))
                            .then();

                    return Mono.when(listenMessages);
                })
                .block();
    }
}
