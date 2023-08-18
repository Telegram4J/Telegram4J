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
