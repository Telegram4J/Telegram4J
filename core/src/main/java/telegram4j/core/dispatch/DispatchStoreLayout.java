package telegram4j.core.dispatch;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.TelegramClient;
import telegram4j.core.store.StoreAction;
import telegram4j.core.store.action.update.UpdateActions;
import telegram4j.json.UpdateData;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public final class DispatchStoreLayout {

    private static final List<DispatchStoreHandler> handlers = new ArrayList<>();

    static {
        addHandler(data -> data.message().isPresent(), data -> UpdateActions.messageCreate(
                data.message().orElseThrow(IllegalStateException::new)));

        addHandler(data -> data.editedMessage().isPresent(), data -> UpdateActions.messageUpdate(
                data.editedMessage().orElseThrow(IllegalStateException::new)));
    }

    private final TelegramClient client;

    public DispatchStoreLayout(TelegramClient client) {
        this.client = client;
    }

    private static void addHandler(Predicate<UpdateData> predicate, Function<UpdateData, StoreAction<?>> actionFactory) {
        handlers.add(new DispatchStoreHandler(predicate, actionFactory));
    }

    @SuppressWarnings("unchecked")
    public <O> Mono<UpdateContext<O>> store(UpdateData data) {
        return Flux.fromIterable(handlers)
                .filter(handler -> handler.predicate.test(data))
                .singleOrEmpty()
                .map(handler -> handler.actionFactory.apply(data))
                .flatMap(action -> (Mono<O>) Mono.from(client.getClientResources().getStore().execute(action)))
                .map(oldData -> UpdateContext.of(data, oldData, client))
                .defaultIfEmpty(UpdateContext.of(data, null, client));
    }

    static class DispatchStoreHandler{
        private final Predicate<UpdateData> predicate;
        private final Function<UpdateData, StoreAction<?>> actionFactory;

        DispatchStoreHandler(Predicate<UpdateData> predicate, Function<UpdateData, StoreAction<?>> actionFactory) {
            this.predicate = predicate;
            this.actionFactory = actionFactory;
        }
    }
}
