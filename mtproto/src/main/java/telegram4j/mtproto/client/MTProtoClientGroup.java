package telegram4j.mtproto.client;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.DataCenter;
import telegram4j.mtproto.DcId;
import telegram4j.tl.api.TlMethod;

import java.util.Objects;

/** The group of MTProto clients which associated to one user.  */
public interface MTProtoClientGroup {

    /**
     * Gets main MTProto client which can be used to received lifetime updates
     * and interact with methods.
     *
     * @return The {@link MTProtoClient main client}.
     */
    MTProtoClient main();

    /**
     * Configures a new main client for this group. Old client will be closed.
     *
     * @param dc The dc to which main client will associate.
     * @return A {@link Mono} emitting on successful completion new main client.
     */
    Mono<MTProtoClient> setMain(DataCenter dc);

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

    UpdateDispatcher updates();

    /**
     * Starts a service task to automatically disconnect inactive clients
     *
     * @return A {@link Mono} emitting empty signals on group close.
     */
    Mono<Void> start();

    /**
     * Closes all underling clients, including lead one.
     *
     * @return A {@link Mono} emitting empty signals on completion.
     */
    Mono<Void> close();

    /**
     * Searches for the client by specified id and if no result found
     * creates a new one and awaits for him connection.
     *
     * @param id The id of client.
     */
    Mono<MTProtoClient> getOrCreateClient(DcId id);

    interface Options {

        static Options of(DataCenter mainDc, ClientFactory clientFactory,
                          UpdateDispatcher updateDispatcher,
                          MTProtoOptions mtProtoOptions) {
            return new OptionsImpl(mainDc, clientFactory, updateDispatcher, mtProtoOptions);
        }

        DataCenter mainDc();

        ClientFactory clientFactory();

        UpdateDispatcher updateDispatcher();

        MTProtoOptions mtProtoOptions();
    }
}

record OptionsImpl(DataCenter mainDc, ClientFactory clientFactory,
                   UpdateDispatcher updateDispatcher,
                   MTProtoOptions mtProtoOptions) implements MTProtoClientGroup.Options {
    OptionsImpl {
        Objects.requireNonNull(mainDc);
        Objects.requireNonNull(clientFactory);
        Objects.requireNonNull(updateDispatcher);
        Objects.requireNonNull(mtProtoOptions);
    }
}
