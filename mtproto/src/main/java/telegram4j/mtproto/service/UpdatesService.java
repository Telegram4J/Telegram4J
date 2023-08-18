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
package telegram4j.mtproto.service;

import reactor.core.publisher.Mono;
import telegram4j.mtproto.client.MTProtoClientGroup;
import telegram4j.mtproto.service.Compatible.Type;
import telegram4j.mtproto.store.StoreLayout;
import telegram4j.tl.request.updates.GetChannelDifference;
import telegram4j.tl.request.updates.GetDifference;
import telegram4j.tl.request.updates.GetState;
import telegram4j.tl.updates.ChannelDifference;
import telegram4j.tl.updates.Difference;
import telegram4j.tl.updates.State;

@Compatible(Type.BOTH)
public class UpdatesService extends RpcService {

    public UpdatesService(MTProtoClientGroup clientGroup, StoreLayout storeLayout) {
        super(clientGroup, storeLayout);
    }

    // updates namespace
    // =========================

    /**
     * Retrieve a current state of updates.
     *
     * @return A {@link Mono} emitting on successful completion current state of updates.
     */
    public Mono<State> getState() {
        return sendMain(GetState.instance());
    }

    /**
     * Retrieve a <b>common</b> updates difference from specified parameters.
     *
     * @return A {@link Mono} emitting on successful completion difference in the <b>common</b> updates from specified parameters.
     */
    public Mono<Difference> getDifference(GetDifference request) {
        return sendMain(request);
    }

    /**
     * Retrieve a <b>channel</b> updates difference from specified parameters.
     *
     * @return A {@link Mono} emitting on successful completion difference in the <b>channel</b> updates from specified parameters.
     */
    public Mono<ChannelDifference> getChannelDifference(GetChannelDifference request) {
        return sendMain(request);
    }
}
