package telegram4j.core.store;

import org.reactivestreams.Publisher;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class ActionMapper {

    private final Map<Class<? extends StoreAction<?>>, ? extends Function<StoreAction<?>, Publisher<?>>> mappings;

    private ActionMapper(Map<Class<? extends StoreAction<?>>, ? extends Function<StoreAction<?>, Publisher<?>>> mappings) {
        this.mappings = mappings;
    }

    public static Builder builder() {
        return new Builder();
    }

    @SuppressWarnings("unchecked")
    public <R> Optional<Function<StoreAction<R>, ? extends Publisher<R>>> findHandlerForAction(StoreAction<R> action) {
        return Optional.ofNullable(mappings.get(action.getClass()))
                .map(handler -> a -> (Publisher<R>) handler.apply(a));
    }

    public static class Builder {

        private final Map<Class<? extends StoreAction<?>>, Function<StoreAction<?>, Publisher<?>>> mappings = new HashMap<>();

        @SuppressWarnings("unchecked")
        public <R, S extends StoreAction<R>> Builder map(Class<S> actionType, Function<? super S, ? extends Publisher<R>> handler) {
            mappings.put(actionType, action -> handler.apply((S) action));
            return this;
        }

        public ActionMapper build() {
            return new ActionMapper(mappings);
        }
    }
}
