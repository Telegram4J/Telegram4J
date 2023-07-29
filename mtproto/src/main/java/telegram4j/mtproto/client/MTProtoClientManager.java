package telegram4j.mtproto.client;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.DataCenter;

/** The {@code MTProtoClientGroup} that can modify their clients. */
public interface MTProtoClientManager extends MTProtoClientGroup {

    /**
     * Configures a new main client for this group. Old client will be closed.
     *
     * @param dc The dc to which main client will associate.
     * @return A {@link Mono} emitting on successful completion new main client.
     */
    Mono<MTProtoClient> setMain(DataCenter dc);
}
