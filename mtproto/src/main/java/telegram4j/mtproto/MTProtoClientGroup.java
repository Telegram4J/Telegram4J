package telegram4j.mtproto;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.api.TlMethod;

/** The group of MTProto clients which associated to one user.  */
public interface MTProtoClientGroup {

    /**
     * Gets main MTProto client which can be used to received lifetime updates
     * and interact with methods.
     *
     * @return The {@link MainMTProtoClient main client}.
     */
    MainMTProtoClient main();

    /**
     * Gets {@link DcId} of {@link #main()} client.
     *
     * @return The {@code DcId} of lead client.
     */
    DcId mainId();

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
     * Gets current list of DC options which used to create clients.
     *
     * @return The list of DC options which used to create clients.
     */
    DcOptions getDcOptions();

    /**
     * Starts all service tasks and loads {@link #getDcOptions()} from {@link StoreLayout}.
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
     * Updates DC options list to specified container. This method will
     * persist new options list to {@link StoreLayout}.
     *
     * @param dcOptions The new options list.
     * @return A {@link Mono} emitting empty signals after list saving.
     */
    Mono<Void> setDcOptions(DcOptions dcOptions);

    /**
     * Searches for the client by specified id and if no result found
     * creates a new one and awaits for him connection.
     *
     * @throws IllegalArgumentException if {@code id} have type {@link DcId.Type#MAIN} and isn't equal to {@link #mainId()}.
     * @param id The id of client.
     */
    Mono<MTProtoClient> getOrCreateClient(DcId id);
}
