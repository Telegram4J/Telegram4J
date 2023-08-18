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
package telegram4j.core.auth;

import reactor.core.publisher.Mono;
import telegram4j.core.AuthorizationResources;
import telegram4j.mtproto.client.MTProtoClientManager;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.auth.BaseAuthorization;

import java.util.Objects;

/** Base interface for implementing auth flow. */
@FunctionalInterface
public interface AuthorizationHandler {

    /**
     * Begins user authorization with specified resources.
     * Implementation may emit empty signals to disconnect client and
     * cancel bootstrap.
     *
     * @param resources The resources for authorisation.
     * @return A {@link Mono} which emits {@link BaseAuthorization} on successful completion or empty signals
     * to cancel bootstrap and close client.
     */
    Mono<BaseAuthorization> process(Resources resources);

    /**
     * Value-based record with components available on auth-flow.
     *
     * @param clientGroup The client group to handle redirections and requests.
     * @param storeLayout The initialized store layout for client.
     * @param authResources The {@code apiId} and {@code apiHash} parameters of application.
     */
    record Resources(MTProtoClientManager clientGroup, StoreLayout storeLayout,
                     AuthorizationResources authResources) {

        public Resources {
            Objects.requireNonNull(clientGroup);
            Objects.requireNonNull(storeLayout);
            Objects.requireNonNull(authResources);
        }
    }
}
