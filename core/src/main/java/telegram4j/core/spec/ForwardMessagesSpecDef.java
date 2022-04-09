package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.util.PeerId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Value.Immutable
interface ForwardMessagesSpecDef extends Spec {

    @Value.Default
    default boolean silent() {
        return false;
    }

    @Value.Default
    default boolean background() {
        return false;
    }

    @Value.Default
    default boolean myScore() {
        return false;
    }

    @Value.Default
    default boolean dropAuthor() {
        return false;
    }

    @Value.Default
    default boolean dropMediaCaptions() {
        return false;
    }

    @Value.Default
    default boolean noForwards() {
        return false;
    }

    List<Integer> ids();

    Optional<Instant> scheduleTimestamp();

    Optional<PeerId> sendAs();
}
