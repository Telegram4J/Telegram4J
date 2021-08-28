package telegram4j.core;

import com.fasterxml.jackson.databind.JsonNode;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.concurrent.Queues;
import telegram4j.core.dispatch.UpdateContext;
import telegram4j.core.event.Event;
import telegram4j.json.UpdateData;
import telegram4j.json.request.GetUpdates;
import telegram4j.rest.*;
import telegram4j.rest.route.Route;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class TelegramClient {
    private static final Logger log = Loggers.getLogger(TelegramClient.class);

    private final RestResources restResources;
    private final RestTelegramClient restClient;
    private final ClientResources clientResources;

    private final AtomicInteger lastUpdateId = new AtomicInteger();
    private final AtomicBoolean terminate = new AtomicBoolean(true);
    private final Sinks.Many<UpdateData> updates;

    TelegramClient(String token, RestResources restResources, ClientResources clientResources) {
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
                    .flatMap(l -> getRestClient().getApplicationService()
                            .getUpdates(GetUpdates.builders()
                                    .offset(terminate.get() ? lastUpdateId.get() + 1 : lastUpdateId.get())
                                    .build())
                            .checkpoint("Read updates from API")
                            .filter(data -> !terminate.get())
                            .filter(data -> data.updateId() > lastUpdateId.get())
                            .collectList()
                            .doOnNext(list -> lastUpdateId.set(list.stream()
                                    .mapToInt(UpdateData::updateId)
                                    .max().orElseGet(lastUpdateId::get)))
                            .doOnNext(list -> list.forEach(updates::tryEmitNext))
                            .doFirst(() -> {
                                synchronized (terminate) {
                                    terminate.set(!terminate.get());
                                }
                            }))
                    .then();

            Mono<Void> mapEvents = updates.asFlux()
                    .flatMap(data -> clientResources.getDispatchMapper().<Event>handle(
                            new UpdateContext(data, this)))
                    .checkpoint("Map updates to events")
                    .doOnNext(clientResources.getEventDispatcher()::publish)
                    .then();

            return Mono.when(readUpdates, mapEvents)
                    .doFirst(() -> log.info("Started updates listening."));
        });
    }
}
