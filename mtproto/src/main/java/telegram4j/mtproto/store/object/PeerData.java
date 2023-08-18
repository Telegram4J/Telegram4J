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
package telegram4j.mtproto.store.object;

import reactor.util.annotation.Nullable;
import telegram4j.tl.api.TlObject;

import java.util.Objects;

public class PeerData<M extends TlObject, F extends TlObject> {
    public final M minData;
    @Nullable
    public final F fullData;

    public PeerData(M minData, @Nullable F fullData) {
        this.minData = Objects.requireNonNull(minData);
        this.fullData = fullData;
    }
}
