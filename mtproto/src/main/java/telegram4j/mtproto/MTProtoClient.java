package telegram4j.mtproto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import telegram4j.tl.Updates;
import telegram4j.tl.api.MTProtoObject;
import telegram4j.tl.api.TlMethod;
import telegram4j.tl.request.messages.SendMessage;

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
     * Gets a {@link Sinks.Many} which redistributes api updates to subscribers and
     * which it can be used to resend updates, as is the case with {@link SendMessage} mapping.
     *
     * @return A {@link Sinks.Many} which redistributes api updates.
     */
    Sinks.Many<Updates> updates();

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
     * Gets the client type.
     *
     * @return The {@link Type} of client.
     */
    Type getType();

    // TODO: It shouldn't be in a public api
    /**
     * Update time offset which is used in message id generation.
     * This method is included in the interface, since the
     * authorization key generation apply primary time synchronization
     *
     * @param serverTime The server unix time in seconds.
     */
    void updateTimeOffset(long serverTime);

    /**
     * Gets calculated server time offset used in message id generation.
     *
     * @return The calculated server time offset.
     */
    int getTimeOffset();

    /**
     * Gets id of current mtproto session.
     *
     * @return The id of current mtproto session
     */
    long getSessionId();

    /**
     * Gets current packet sequence number.
     *
     * @return The current packet sequence number.
     */
    int getSeqNo();

    /**
     * Gets active server salt.
     *
     * @return The active server salt.
     */
    long getServerSalt();

    /**
     * Create child media client, associated with given datacenter.
     *
     * @param dc The media datacenter.
     * @return The new child media client.
     */
    MTProtoClient createMediaClient(DataCenter dc);

    /**
     * Create child client, associated with parent datacenter.
     *
     * @return The new child client.
     */
    MTProtoClient createChildClient();

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

        /**
         * The state in which the client is connected to
         * the dc and ready to send and receive packets.
         */
        CONNECTED,

        /** The state in which the client is starts the auth key gen. */
        AUTHORIZATION_BEGIN,

        /** The state in which the client is ends the auth key gen. */
        AUTHORIZATION_END,

        /** The intermediate state indicating reconnection of the client to the dc. */
        RECONNECT
    }

    // TODO: maybe remove and use DataCenter#getType()?
    /** Available client types and modes. */
    enum Type {
        /** The regular mtproto client. */
        DEFAULT,

        /**
         * The client mode in which client
         * must apply auth key from parent and upload/download
         * parts of the file from media dcs.
         */
        MEDIA,

        /**
         * The client mode in which client
         * must generate auth key for cdn dcs
         * and downloads files from cdn dcs.
         */
        CDN
    }
}
