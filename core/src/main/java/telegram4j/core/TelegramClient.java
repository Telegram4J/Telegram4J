package telegram4j.core;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.Logger;
import reactor.util.Loggers;
import reactor.util.concurrent.Queues;
import telegram4j.core.dispatch.DispatchStoreLayout;
import telegram4j.core.event.Event;
import telegram4j.core.object.Message;
import telegram4j.core.object.Poll;
import telegram4j.core.spec.*;
import telegram4j.json.MessageData;
import telegram4j.json.UpdateData;
import telegram4j.json.request.*;
import telegram4j.rest.DefaultRouter;
import telegram4j.rest.RestResources;
import telegram4j.rest.RestTelegramClient;
import telegram4j.rest.RouterResources;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class TelegramClient {
    private static final Logger log = Loggers.getLogger(TelegramClient.class);

    private final RestResources restResources;
    private final RestTelegramClient restClient;
    private final ClientResources clientResources;

    private final AtomicInteger lastUpdateId = new AtomicInteger(-1);
    private final AtomicBoolean terminate = new AtomicBoolean(true);
    private final AtomicBoolean request = new AtomicBoolean();
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


    public Mono<Message> editMessageText(MessageEditTextSpec spec) {
        return getRestClient().getChatService()
                .editMessageText(spec.asRequest())
                .filter(node -> !node.isBoolean())
                .map(node -> getRestResources().getObjectMapper()
                        .convertValue(node, MessageData.class))
                .map(data -> new Message(this, data));
    }

    public Mono<Message> editMessageCaption(MessageEditCaptionSpec spec) {
        return getRestClient().getChatService()
                .editMessageCaption(spec.asRequest())
                .filter(node -> !node.isBoolean())
                .map(node -> getRestResources().getObjectMapper()
                        .convertValue(node, MessageData.class))
                .map(data -> new Message(this, data));
    }

    public Mono<Message> editMessageMedia(MessageEditMediaSpec spec) {
        return getRestClient().getChatService()
                .editMessageMedia(spec.asRequest())
                .filter(node -> !node.isBoolean())
                .map(node -> getRestResources().getObjectMapper()
                        .convertValue(node, MessageData.class))
                .map(data -> new Message(this, data));
    }

    public Mono<Message> editMessageReplyMarkup(MessageEditReplyMarkupSpec spec) {
        return getRestClient().getChatService()
                .editMessageReplyMarkup(spec.asRequest())
                .filter(node -> !node.isBoolean())
                .map(node -> getRestResources().getObjectMapper()
                        .convertValue(node, MessageData.class))
                .map(data -> new Message(this, data));
    }

    public Mono<Poll> stopPoll(StopPollSpec spec) {
        return getRestClient().getChatService().stopPoll(spec.asRequest())
                .map(data -> new Poll(this, data));
    }

    public Mono<Void> login() {
        return Mono.defer(() -> {

            Mono<Void> readUpdates = Flux.interval(clientResources.getUpdateInterval())
                    .flatMap(l -> {
                        if (request.get()) {
                            return Mono.empty();
                        }

                        request.set(true);
                        return Mono.fromSupplier(terminate::get)
                                .filter(bool -> !bool)
                                .switchIfEmpty(getRestClient().getApplicationService()
                                        .getUpdates(GetUpdates.builders()
                                                .offset(lastUpdateId.get() + 1)
                                                .build())
                                        .checkpoint("Terminate API updates")
                                        .doFirst(() -> terminate.set(false))
                                        .then(Mono.empty()))
                                .flatMapMany(bool -> getRestClient().getApplicationService()
                                        .getUpdates(GetUpdates.builders()
                                                .offset(lastUpdateId.get())
                                                .build()))
                                .checkpoint("Read updates from API")
                                .filter(data -> data.updateId() > lastUpdateId.get())
                                .collectList()
                                .doOnNext(list -> lastUpdateId.set(list.stream()
                                        .mapToInt(UpdateData::updateId)
                                        .max().orElseGet(lastUpdateId::get)))
                                .doOnNext(list -> list.forEach(updates::tryEmitNext))
                                .doOnNext(list -> terminate.set(!list.isEmpty()))
                                .then(Mono.fromRunnable(() -> request.set(false)));
                    })
                    .then();

            DispatchStoreLayout dispatchStoreLayout = new DispatchStoreLayout(this);

            Mono<Void> mapEvents = updates.asFlux()
                    .flatMap(dispatchStoreLayout::store)
                    .checkpoint("Save updates to store")
                    .<Event>flatMap(clientResources.getDispatchMapper()::handle)
                    .checkpoint("Map updates to events")
                    .doOnNext(clientResources.getEventDispatcher()::publish)
                    .then();

            return Mono.when(readUpdates, mapEvents)
                    .doFirst(() -> log.info("Started updates listening."));
        });
    }
}
