package telegram4j.core.store;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import telegram4j.core.store.action.read.GetMessage;
import telegram4j.core.store.action.update.MessageCreateAction;
import telegram4j.core.store.action.update.MessageUpdateAction;

public final class Store {

    private final ActionMapper actionMapper;

    private Store(ActionMapper actionMapper) {
        this.actionMapper = actionMapper;
    }

    public static Store fromLayout(StoreLayout storeLayout) {
        return new Store(ActionMapper.builder()
                .map(MessageCreateAction.class, action -> storeLayout.onMessageCreate(action.getMessageData()))
                .map(MessageUpdateAction.class, action -> storeLayout.onMessageUpdate(action.getMessageData()))
                .map(GetMessage.class, action -> storeLayout.getMessageById(action.getChatId(), action.getMessageId()))
                .build());
    }

    public <R> Publisher<R> execute(StoreAction<R> action) {
        return actionMapper.findHandlerForAction(action)
                .<Publisher<R>>map(h -> h.apply(action))
                .orElse(Flux.empty());
    }
}
