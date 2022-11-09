package telegram4j.mtproto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import telegram4j.tl.api.MTProtoObject;
import telegram4j.tl.api.TlMethod;

/**
 * Interface for MTProto client implementations with minimal method set.
 * @implSpec The client must implement RPC, auth, update logics and auto-reconnects.
 */
public interface MTProtoClient {

    /**
     * Gets a {@link Mono} with empty signals which starts client on subscribe.
     *
     * @return A {@link Mono} which emitting signals on client close.
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
     * Send auth api request for which result is not applicable.
     *
     * @param method An auth api request.
     * @return A {@link Mono} emitting empty signals immediately after request is prepared.
     */
    Mono<Void> sendAuth(TlMethod<? extends MTProtoObject> method);

    /**
     * Gets a {@link Flux} of {@link State} displaying current status of the client.
     *
     * @return A {@link Flux} emitting a {@link State}.
     */
    Flux<State> state();

    /**
     * Gets the client datacenter.
     *
     * @return The {@link DataCenter} to which the client is configured.
     */
    DataCenter getDatacenter();

    /**
     * Gets current info about mtproto session.
     *
     * @return The current info about mtproto session.
     */
    SessionInfo getSessionInfo();

    /**
     * Gets a {@link Mono} which closes client and emitting empty signals.
     *
     * @return A {@link Mono} emitting empty signals on successful completion.
     */
    Mono<Void> close();

    /** Available client states. */
    enum State {
        /** The state in which the client must reconnect. */
        DISCONNECTED,

        /** The state in which the client must fully shutdown without the possibility of resuming. */
        CLOSED,

        /** The state indicates client's willingness to send requests, i.e. after sending a transport id. */
        CONFIGURED,

        /** The state in which the client is connected to the dc. */
        CONNECTED,

        /** The state in which the client is ready to send requests to the dc. */
        READY,

        /** The state in which the client is starts the auth key gen. */
        AUTHORIZATION_BEGIN,

        /** The state in which the client is ends the auth key gen. */
        AUTHORIZATION_END,

        /** The intermediate state indicating reconnection of the client to the dc. */
        RECONNECT
    }
}
