package telegram4j.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.concurrent.Queues;
import telegram4j.core.dispatch.UpdateContext;
import telegram4j.core.event.Event;
import telegram4j.json.UpdateData;
import telegram4j.rest.DefaultRouter;
import telegram4j.rest.RestResources;
import telegram4j.rest.RestTelegramClient;
import telegram4j.rest.RouterResources;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;

public final class TelegramClient {

    private final String token;
    private final RestResources restResources;
    private final RestTelegramClient restClient;
    private final ClientResources clientResources;

    private final AtomicInteger lastUpdateId = new AtomicInteger();
    private final Sinks.Many<UpdateData> updates;

    TelegramClient(String token, RestResources restResources, ClientResources clientResources) {
        this.token = token;
        this.restResources = restResources;
        this.restClient = new RestTelegramClient(new DefaultRouter(
                new RouterResources(token, restResources)));
        this.clientResources = clientResources;

        this.updates = newManySink();
    }

    private static <T> Sinks.Many<T> newManySink() {
        return Sinks.many().multicast().onBackpressureBuffer(Queues.SMALL_BUFFER_SIZE, false);
    }

    public static TelegramClient create(String token) {
        return builder().setToken(token).build();
    }

    public static TelegramClientBuilder builder() {
        return new TelegramClientBuilder();
    }

    public RestResources getRestResources() {
        return restResources;
    }

    public RestTelegramClient getRestClient() {
        return restClient;
    }

    public ClientResources getClientResources() {
        return clientResources;
    }

    public <E extends Event> Flux<E> on(Class<E> type) {
        return clientResources.getEventDispatcher().on(type);
    }

    public Mono<Void> login() {
        return Mono.defer(() -> {

            Mono<Void> readUpdates = Flux.interval(clientResources.getUpdateInterval())
                    .flatMap(l -> getRestClient().getApplicationService().getUpdates()
                            .filter(data -> data.updateId() > lastUpdateId.get())
                            .sort(Comparator.comparingInt(UpdateData::updateId))
                            .doOnNext(data -> lastUpdateId.set(data.updateId()))
                            .checkpoint("Read updates from API")
                            .doOnNext(updates::tryEmitNext))
                    .then();

            Mono<Void> mapEvents = updates.asFlux()
                    .flatMap(data -> clientResources.getDispatchMapper().<Event>handle(
                            new UpdateContext(data, this)))
                    .checkpoint("Map updates to events")
                    .doOnNext(clientResources.getEventDispatcher()::publish)
                    .then();

            return Mono.when(readUpdates, mapEvents);
        });
    }
}
