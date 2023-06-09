package telegram4j.mtproto.client;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcId;
import telegram4j.tl.api.TlMethod;

import java.time.Instant;
import java.util.Optional;

/**
 * Interface for MTProto client implementations with minimal method set.
 *
 * @implSpec The client must implement RPC, auth, update logics and auto-reconnects.
 */
public interface MTProtoClient {

    /**
     * Gets a {@link Mono} with empty signals which starts client on subscribe.
     *
     * @return A {@link Mono} which emitting signals when client ready to handle requests.
     */
    Mono<Void> connect();

    /**
     * Send api request with result awaiting.
     *
     * @param <T> Type of method.
     * @param <R> Type of result.
     * @param method An api request.
     * @return A {@link Mono} emitting signals with result on successful completion.
     */
    <R, T extends TlMethod<R>> Mono<R> sendAwait(T method);

    /**
     * Gets the client datacenter.
     *
     * @return The {@link DataCenter} to which the client is configured.
     */
    DataCenter dc();

    DcId.Type type();

    /**
     * Gets mutable statistic for client.
     *
     * @return The statistic for client.
     */
    Stats stats();

    /**
     * Gets a {@link Mono} which closes client and emitting empty signals.
     *
     * @return A {@link Mono} emitting empty signals on successful completion.
     */
    Mono<Void> close();

    Mono<Void> onClose();

    /** Interface for client statistic. */
    interface Stats {

        /**
         * Gets timestamp of last {@link #sendAwait(TlMethod)} call, if present.
         *
         * @return The timestamp of last send query call, if present.
         */
        Optional<Instant> lastQueryTimestamp();

        /**
         * Gets current count of pending queries.
         *
         * @return The current count of pending queries.
         */
        int queriesCount();

        /**
         * Creates new immutable copy of this statistics.
         *
         * @return A new immutable copy of this statistics.
         */
        default Stats copy() {
            return new ImmutableStats(lastQueryTimestamp().orElse(null), queriesCount());
        }
    }
}
