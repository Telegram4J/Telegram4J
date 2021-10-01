package telegram4j.core.dispatch;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.core.event.Event;

import java.util.ArrayList;
import java.util.List;

public class DefaultDispatchMapper implements DispatchMapper {

    private static final List<DispatchHandler<?, ?>> handlers = new ArrayList<>();

    static {
        handlers.add(new ChatDispatchHandlers.MessageCreate());
        handlers.add(new ChatDispatchHandlers.MessageUpdate());
        handlers.add(new ChatDispatchHandlers.ChannelPostCreate());
        handlers.add(new ChatDispatchHandlers.ChannelPostUpdate());
        handlers.add(new ChatDispatchHandlers.PollCreate());
        handlers.add(new ChatDispatchHandlers.PollAnswerCreate());
        handlers.add(new QueryDispatchHandlers.InlineQueryCreate());
        handlers.add(new QueryDispatchHandlers.ChosenInlineResultCreate());
        handlers.add(new QueryDispatchHandlers.CallbackQueryCreate());
        handlers.add(new QueryDispatchHandlers.ShippingQueryCreate());
        handlers.add(new QueryDispatchHandlers.PreCheckoutQueryCreate());
        handlers.add(new ChatMemberDispatcherHandlers.SelfChatMemberUpdate());
        handlers.add(new ChatMemberDispatcherHandlers.ChatMemberUpdate());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends Event, O> Mono<E> handle(UpdateContext<O> update) {
        return Flux.fromIterable(handlers)
                .map(handler -> (DispatchHandler<E, O>) handler)
                .filter(handler -> handler.canHandle(update))
                .singleOrEmpty()
                .flatMap(handler -> handler.handle(update));
    }
}
