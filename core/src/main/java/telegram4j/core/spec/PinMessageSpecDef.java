package telegram4j.core.spec;

import org.immutables.value.Value;

@Value.Immutable(singleton = true)
interface PinMessageSpecDef extends Spec {

    @Value.Default
    default boolean silent() {
        return false;
    }

    @Value.Default
    default boolean unpin() {
        return false;
    }

    @Value.Default
    default boolean pmOneSide() {
        return false;
    }
}
