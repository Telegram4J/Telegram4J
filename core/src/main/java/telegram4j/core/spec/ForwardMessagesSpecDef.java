package telegram4j.core.spec;

import org.immutables.value.Value;
import telegram4j.core.object.PeerId;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Value.Immutable
interface ForwardMessagesSpecDef extends Spec {

    default boolean silent() {
        return false;
    }

    default boolean background() {
        return false;
    }

    default boolean withMyScore() {
        return false;
    }

    default boolean dropAuthor() {
        return false;
    }

    default boolean dropMediaCaptions() {
        return false;
    }

    default boolean noForwards() {
        return false;
    }

    List<Integer> ids();

    Optional<Instant> scheduleTimestamp();

    Optional<PeerId> sendAs();
}
