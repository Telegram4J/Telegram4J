package telegram4j.mtproto;

import reactor.core.publisher.Mono;
import telegram4j.tl.api.TlMethod;

import java.util.Optional;

/** The group of MTProto clients which associated to one user.  */
public interface MTProtoClientGroup {

    /**
     * Gets main MTProto client which can be used to received lifetime updates
     * and interact with methods.
     *
     * @return The {@link MainMTProtoClient main MTProto client}.
     */
    MainMTProtoClient main();

    /**
     * Gets {@link DcId} of {@link #main() lead} client.
     *
     * @return The {@code DcId} of lead client.
     */
    DcId mainId();

    /**
     * Gets {@code MTProtoClient} by specified id.
     *
     * @param id The id of client.
     * @return The {@code MTProtoClient} client, if found.
     */
    Optional<MTProtoClient> find(DcId id);

    /**
     * Sends TL method to specified datacenter.
     *
     * @param <M> The type of TL method.
     * @param <R> The return type of method.
     * @param id The id of client.
     * @param method The method to send.
     * @see MTProtoClient#sendAwait(TlMethod)
     * @return A {@link Mono} emitting signals with result on successful completion.
     */
    <R, M extends TlMethod<R>> Mono<R> send(DcId id, M method);

    /**
     * Closes all underling clients, including lead one.
     *
     * @return A {@link Mono} emitting empty signals on completion.
     */
    Mono<Void> close();
}
