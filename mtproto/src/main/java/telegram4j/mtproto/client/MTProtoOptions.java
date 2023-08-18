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

import telegram4j.mtproto.PublicRsaKeyRegister;
import telegram4j.mtproto.auth.DhPrimeChecker;
import telegram4j.mtproto.resource.TcpClientResources;
import telegram4j.mtproto.store.StoreLayout;

import java.util.concurrent.ExecutorService;

import static java.util.Objects.requireNonNull;

public record MTProtoOptions(TcpClientResources tcpClientResources, PublicRsaKeyRegister publicRsaKeyRegister,
                             DhPrimeChecker dhPrimeChecker, StoreLayout storeLayout,
                             ExecutorService resultPublisher, boolean disposeResultPublisher) {

    public MTProtoOptions {
        requireNonNull(tcpClientResources);
        requireNonNull(publicRsaKeyRegister);
        requireNonNull(dhPrimeChecker);
        requireNonNull(storeLayout);
        requireNonNull(resultPublisher);
    }
}
