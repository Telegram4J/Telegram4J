/*
 * Copyright 2023 Telegram4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
     * Sends TL method to specified datacenter.
     *
     * @param <R> The return type of method.
     * @param id The id of client.
     * @param method The method to send.
     * @see MTProtoClient#send(TlMethod)
     * @return A {@link Mono} emitting signals with result on successful completion.
     */
    <R> Mono<R> send(DcId id, TlMethod<? extends R> method);

    /** {@return A dispatcher for life updates} */
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
